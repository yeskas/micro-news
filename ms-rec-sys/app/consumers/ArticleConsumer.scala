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
case class Article(id: Int, tags: List[String])


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

		def addArticle(id: Int, articleTags: List[String], articleJson: String) : Unit = {
			// got cash?
			session.execute("" +
				s"INSERT INTO test01.articles (id, article_json) " +
				s"VALUES ($id, $$$$$articleJson$$$$)"
			)

			val tags_csv = articleTags.mkString(", ")
			val vals_csv = Array.fill(articleTags.length)(1).mkString(", ")

			session.execute("" +
				s"INSERT INTO test01.article_tags (id, ${tags_csv}) " +
				s"VALUES ($id, ${vals_csv})"
			)

			// Prepend to the default cluster
			// TODO: improve code; add to other best-matching clusters
			val rs = session.execute(s"SELECT * FROM test01.clusters where id = 0")
			val row = rs.one()
			val oldJson = row.getString("articles_json")
			val newJson = "[" + articleJson + ", " + oldJson.substring(1)
			session.execute("" +
				s"UPDATE test01.clusters " +
				s"SET articles_json = $$$$$newJson$$$$" +
				s"WHERE id = 0"
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
				val articleJson = new String(body, "UTF-8")
				println("Received message: " + articleJson)

				// Parse out the article object
				implicit val formats = DefaultFormats
				val json = parse(articleJson)
				val article = json.extract[Article]

				CassandraClient.addArticle(article.id, article.tags, articleJson)

				println("Done with message\n\n\n")
				channel.basicAck(envelope.getDeliveryTag, false)
			}
		}

		// Block for new messages
		channel.basicConsume(IN_TASK_QUEUE_NAME, false, consumer)
	}

}
