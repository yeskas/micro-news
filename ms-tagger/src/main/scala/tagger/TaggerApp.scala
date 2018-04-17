package tagger


import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.rabbitmq.client.{AMQP, ConnectionFactory, DefaultConsumer, Envelope, MessageProperties}


object TaggerApp {
	private val IN_TASK_QUEUE_NAME = "downloader:tagger:articles"

	private val OUT_TASK_QUEUE_NAME = "tagger:rec-sys:articles"

	def main(argv: Array[String]): Unit = {
		println("STARTING THE TAGGER")

		val tagger = new Tagger("config/tags.json")

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
			override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
				val message = new String(body, "UTF-8")
				println("Received message: " + message)

				// Parse out the article object
				implicit val formats = DefaultFormats
				val json = parse(message)
				val article = json.extract[Article]

				// Assign tags
				val tags = tagger.tagArticle(article)

				// Forward to rec sys
				if (tags.length > 0) {
					println("Assigned some tags, need to send to REC-SYS:")
					println(tags.mkString(", "))

					// Send tagged article to REC-SYS
					// TODO: reuse existing connection
					val outConnection = factory.newConnection
					val outChannel = connection.createChannel

					outChannel.queueDeclare(OUT_TASK_QUEUE_NAME, true, false, false, null)

					// Dump as json
					val outMessage = compact(render(
						("id" -> article.id) ~
						("link" -> article.link) ~
						("title" -> article.title) ~
						("body" -> article.body) ~
						("image" -> article.image) ~
						("timestamp" -> article.timestamp) ~
						("source" ->
							("name" -> article.source.name) ~
							("link" -> article.source.link) ~
							("icon" -> article.source.icon)
						) ~
						("tags" -> tags.toList)
					))

					channel.basicPublish(
						"",
						OUT_TASK_QUEUE_NAME,
						MessageProperties.PERSISTENT_TEXT_PLAIN,
						outMessage.getBytes("UTF-8")
					)

					outChannel.close()
					outConnection.close()
					println("Tagged & sent to REC-SYS: " + outMessage)

				} else {
					println("Wasn't able to assign any tags, IGNORING this article")
				}

				println("Done with message\n\n\n")
				channel.basicAck(envelope.getDeliveryTag, false)
			}
		}

		// Block for new messages
		channel.basicConsume(IN_TASK_QUEUE_NAME, false, consumer)
	}
}
