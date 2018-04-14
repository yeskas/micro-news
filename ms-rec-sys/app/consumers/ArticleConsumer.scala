package consumers


import scala.collection.mutable
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


// Fields of interest in the incoming article jsons
case class Article(tags: List[String])


class ArticleConsumerModule extends SimpleModule(bind[ArticleConsumer].toSelf.eagerly())


class ArticleConsumer @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {
	private val IN_TASK_QUEUE_NAME = "tagger:rec-sys:articles"

	// TODO: combine all CassandraClient classes
	object CassandraClient {
		private val cluster = Cluster.builder()
			.addContactPoint("localhost")
			.withPort(9042)
			.build()

		val session = cluster.connect()

		// TODO: don't do this; store in a config table instead; or get during insert;
		def getNextArticleId() : Int = {
			val rs = session.execute("SELECT MAX(id) as max_id FROM test01.articles")
			val row = rs.one()

			row.getInt("max_id") + 1
		}

		def insertArticle(articleJson: String, articleTags: List[String]) : Unit = {
			val newId = getNextArticleId()

			// TODO: rename to article_json
			session.execute("" +
				s"INSERT INTO test01.articles (id, body_json) " +
				s"VALUES ($newId, '$articleJson')"
			)

			val tags_csv = articleTags.mkString(", ")
			val vals_csv = Array.fill(articleTags.length)(1).mkString(", ")

			session.execute("" +
				s"INSERT INTO test01.article_tags (id, ${tags_csv}) " +
				s"VALUES ($newId, ${vals_csv})"
			)
		}

	}

	// Make it start @ app start
	// TODO: find a better way to launch @ startup
	actorSystem.scheduler.schedule(initialDelay = 2.seconds, interval = 100.days) {
		println("---- Starting the ArticleConsumer ----")

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
				val message = new String(body, "UTF-8")
				println("Received message: " + message)

				// Parse out the article object
				implicit val formats = DefaultFormats
				val json = parse(message)
				val article = json.extract[Article]

				CassandraClient.insertArticle(message, article.tags)

				println("Done with message\n\n\n")
				channel.basicAck(envelope.getDeliveryTag, false)
			}
		}

		// Block for new messages
		channel.basicConsume(IN_TASK_QUEUE_NAME, false, consumer)
	}

}
