package tagger


import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.rabbitmq.client.{AMQP, Channel, ConnectionFactory, DefaultConsumer, Consumer, Envelope, MessageProperties}


object TaggerApp {
	private val RABBITMQ_HOST = "localhost"
	private val RABBITMQ_IN_QUEUE = "downloader:tagger:articles"
	private val RABBITMQ_OUT_QUEUE = "tagger:rec-sys:articles"

	// Wrapper around the incoming (untagged articles) and outgoing (tagged articles) channels
	class RabbitMQClient(host: String, inQueue: String, outQueue: String) {
		private val factory = new ConnectionFactory()
		factory.setHost(host)

		private val inConnection = factory.newConnection()
		val inChannel: Channel = inConnection.createChannel()
		inChannel.queueDeclare(inQueue, true, false, false, null)
		inChannel.basicQos(1)

		private val outConnection = factory.newConnection()
		val outChannel: Channel = outConnection.createChannel()
		outChannel.queueDeclare(outQueue, true, false, false, null)

		// Convenience wrapper around basic consume
		def consume(consumer: Consumer): Unit = {
			inChannel.basicConsume(inQueue, false, consumer)
		}

		// Convenience wrapper around basic ack
		def ack(deliveryTag: Long): Unit = {
			inChannel.basicAck(deliveryTag, false)
		}

		// Convenience wrapper around basic publish
		def publish(message: String): Unit = {
			outChannel.basicPublish(
				"",
				outQueue,
				MessageProperties.PERSISTENT_TEXT_PLAIN,
				message.getBytes("UTF-8")
			)
		}

	}

	def main(argv: Array[String]): Unit = {
		println("STARTING THE TAGGER APP")

		val tagger = new Tagger("config/tags.json")

		val rabbitMQClient = new RabbitMQClient(RABBITMQ_HOST, RABBITMQ_IN_QUEUE, RABBITMQ_OUT_QUEUE)
		println("Waiting for messages...")

		// Set up the message handler
		val consumer = new DefaultConsumer(rabbitMQClient.inChannel) {
			override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
				val inMessage = new String(body, "UTF-8")
				println("Received message: " + inMessage)

				// Parse out the article object
				implicit val formats = DefaultFormats
				val json = parse(inMessage)
				val article = json.extract[Article]

				// Assign tags
				val tags = tagger.tagArticle(article)

				if (tags.length > 0) {
					// Send tagged article as json string to REC-SYS
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
					rabbitMQClient.publish(outMessage)
					println("Tagged & sent to REC-SYS: " + outMessage)

				} else {
					println("Wasn't able to assign any tags, IGNORING this article")
				}

				rabbitMQClient.ack(envelope.getDeliveryTag)
				println("Done with message")
			}
		}

		// Block for new messages
		rabbitMQClient.consume(consumer)
	}
}
