package consumers


import scala.collection.mutable
import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.inject.{SimpleModule, _}

import akka.actor.ActorSystem
import javax.inject.Inject

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import com.rabbitmq.client.{AMQP, ConnectionFactory, DefaultConsumer, Envelope, MessageProperties}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._


// Fields of interest in the incoming feedback jsons
case class Feedback(userId: Int, articleId: Int)


class FeedbackConsumerModule extends SimpleModule(bind[FeedbackConsumer].toSelf.eagerly())


class FeedbackConsumer @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {
	private val IN_TASK_QUEUE_NAME = "gateway:rec-sys:feedback"

	// TODO: combine all CassandraClient classes
	object CassandraClient {
		private val cluster = Cluster.builder()
			.addContactPoint("localhost")
			.withPort(9042)
			.build()

		val session = cluster.connect()

		// Adds feedback about user's click on article to the db:
		// - adds user of first feedback
		// - increments user's tags by article's tags
		def addFeedback(feedback: Feedback) : Unit = {
			val userId = feedback.userId
			val articleId = feedback.articleId

			// If first feedback by user, add to users table
			var rs = session.execute(s"SELECT id FROM test01.users where id = $userId")
			if (rs.isExhausted()) {
				session.execute(s"INSERT INTO test01.users (id, cluster_id) VALUES ($userId, 0)")
				session.execute(s"INSERT INTO test01.user_tags (id) VALUES ($userId)")
			}

			// Fetch all non-zero tags of article
			val tagToArticleWeight = mutable.Map[String, Int]()
			rs = session.execute(s"SELECT * FROM test01.article_tags where id = $articleId")
			if (rs.isExhausted()) {
				println("Got feedback on non-existent article")
				return
			}
			var row = rs.one()
			for (columnDef <- row.getColumnDefinitions.asList().asScala) {
				val column = columnDef.getName()
				if (column != "id") {
					if (!row.isNull(column)) {
						tagToArticleWeight(column) = row.getInt(column)
					}
				}
			}

			// Fetch the current values of same tags of user
			val tagsCsv = tagToArticleWeight.keys.mkString(", ")
			rs = session.execute(s"SELECT id, $tagsCsv FROM test01.user_tags where id = $userId")
			row = rs.one()

			// Write updated user tags to db
			val setStatements = mutable.ArrayBuffer[String]()
			for ((tag, articleWeight) <- tagToArticleWeight) {
				var userWeight
				if (row.isNull(tag)) {
					userWeight = 0
				} else {
					userWeight = row.getInt(tag)
				}

				setStatements += s"$tag=${userWeight + articleWeight}"
			}
			session.execute("" +
				s"UPDATE test01.user_tags " +
				s"SET ${setStatements.mkString(", ")} " +
				s"WHERE id = $userId"
			)
		}

	}

	// Make it start @ app start
	// TODO: find a better way to launch @ startup
	actorSystem.scheduler.schedule(initialDelay = 2.seconds, interval = 100.days) {
		println("---- Starting the FeedbackConsumer ----")

		// Initialize RabbitMQ connection
		// TODO: initialize from config
		val factory = new ConnectionFactory()
		factory.setHost("localhost")
		val connection = factory.newConnection()
		val channel = connection.createChannel()
		channel.queueDeclare(IN_TASK_QUEUE_NAME, true, false, false, null)
		channel.basicQos(1)

		println("Waiting for messages...")

		// Set up the message handler
		val consumer = new DefaultConsumer(channel) {
			override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties,
										body: Array[Byte]): Unit = {
				val feedbackJson = new String(body, "UTF-8")
				println("Received message: " + feedbackJson)

				// Parse out the article object
				implicit val formats = DefaultFormats
				val json = parse(feedbackJson)
				val feedback = json.extract[Feedback]

				CassandraClient.addFeedback(feedback)

				println("Done with message\n\n\n")
				channel.basicAck(envelope.getDeliveryTag, false)
			}
		}

		// Block for new messages
		channel.basicConsume(IN_TASK_QUEUE_NAME, false, consumer)
	}

}
