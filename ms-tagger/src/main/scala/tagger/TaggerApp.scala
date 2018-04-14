//#full-example
package tagger


import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

import org.json4s._
import org.json4s.jackson.JsonMethods._

import com.rabbitmq.client.{ConnectionFactory, DefaultConsumer, Envelope, AMQP}


//#greeter-companion
//#greeter-messages
object Greeter {
	//#greeter-messages
	def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
	//#greeter-messages
	final case class WhoToGreet(who: String)
	case object Greet
}
//#greeter-messages
//#greeter-companion


//#greeter-actor
class Greeter(message: String, printerActor: ActorRef) extends Actor {
	import Greeter._
	import Printer._

	var greeting = ""

	def receive = {
		case WhoToGreet(who) =>
			greeting = s"$message, $who"
		case Greet					 =>
			//#greeter-send-message
			printerActor ! Greeting(greeting)
		//#greeter-send-message
	}
}
//#greeter-actor


//#printer-companion
//#printer-messages
object Printer {
	//#printer-messages
	def props: Props = Props[Printer]
	//#printer-messages
	final case class Greeting(greeting: String)
}
//#printer-messages
//#printer-companion


//#printer-actor
class Printer extends Actor with ActorLogging {
	import Printer._

	def receive = {
		case Greeting(greeting) =>
			log.info(s"Greeting received (from ${sender()}): $greeting")
	}
}
//#printer-actor


case class Article(title: String, body: String)

// Info on words related to tag words
case class Link(word: String, score: Double)
case class Tag(word: String, links: List[Link])


class Tagger(tagsFilePath: String) {
	private val allTags = parseTags()

	// Read entire tags file into memory, since it's small in size
	private def parseTags() : List[Tag] = {
		println(s"Parsing tags data from: $tagsFilePath")

		val source = scala.io.Source.fromFile(tagsFilePath)
		val tagsJson = try source.mkString finally source.close()

		implicit val formats = DefaultFormats
		val json = parse(tagsJson)
		json.extract[List[Tag]]
	}

	def tagArticle(article: Article): Array[String] = {
		// parse out all words & count occurrences
		val wordRegex = "(\\w+)".r

		val titleWords = wordRegex.findAllIn(article.title.toLowerCase()).toList
		val bodyWords = wordRegex.findAllIn(article.body.toLowerCase()).toList

		val allWords = titleWords ++ bodyWords
		val wordToOccurrences = allWords.groupBy(word=>word).mapValues(_.size)

		// score each potential tag, based on # of occurrences and it's relevance
		// i.e. scalar multiplication of occurrences-vector & scores-vector
		val tagScores = new ArrayBuffer[(String, Double)]()
		for (tag <- allTags) {
			var score = 0.0

			for (link <- tag.links) {
				val count = wordToOccurrences.getOrElse(link.word, 0)
				score += link.score * count
			}

			println(s"Score for ${tag.word} is $score")
			tagScores += ((tag.word, score))
		}

		// keep top up-to-5 non-zero tags
		val topTags = tagScores
			.toArray
			.filter(tagScore => tagScore._2 > 0)
			.sortWith(_._2 > _._2)
			.slice(0, 5)
			.map(tagScore => tagScore._1)

		topTags
	}
}


//#main-class
object TaggerApp {
	private val TASK_QUEUE_NAME = "downloader:tagger:articles"

	def main(argv: Array[String]): Unit = {
		println("STARTING THE TAGGER")

		val tagger = new Tagger("util/tags.json")

		// Initialize RabbitMQ connection
		val factory = new ConnectionFactory()
		factory.setHost("localhost")
		val connection = factory.newConnection()
		val channel = connection.createChannel()
		channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null)
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

				} else {
					println("Wasn't able to assign any tags, IGNORING this article")
				}

				println("Done with message\n\n\n")
				channel.basicAck(envelope.getDeliveryTag, false)
			}
		}

		// Block for new messages
		channel.basicConsume(TASK_QUEUE_NAME, false, consumer)
	}


	//	import Greeter._
	//
	//	// Create the 'helloAkka' actor system
	//	val system: ActorSystem = ActorSystem("helloAkka")
	//
	//	//#create-actors
	//	// Create the printer actor
	//	val printer: ActorRef = system.actorOf(Printer.props, "printerActor")
	//
	//	// Create the 'greeter' actors
	//	val howdyGreeter: ActorRef =
	//		system.actorOf(Greeter.props("Howdy", printer), "howdyGreeter")
	//	val helloGreeter: ActorRef =
	//		system.actorOf(Greeter.props("Hello", printer), "helloGreeter")
	//	val goodDayGreeter: ActorRef =
	//		system.actorOf(Greeter.props("Good day", printer), "goodDayGreeter")
	//	//#create-actors
	//
	//	//#main-send-messages
	//	howdyGreeter ! WhoToGreet("Akka")
	//	howdyGreeter ! Greet
	//
	//	howdyGreeter ! WhoToGreet("Lightbend")
	//	howdyGreeter ! Greet
	//
	//	helloGreeter ! WhoToGreet("Scala")
	//	helloGreeter ! Greet
	//
	//	goodDayGreeter ! WhoToGreet("Play")
	//	goodDayGreeter ! Greet
	//	//#main-send-messages
}
//#main-class
//#full-example
