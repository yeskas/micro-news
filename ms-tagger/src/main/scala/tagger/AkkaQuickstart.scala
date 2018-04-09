//#full-example
package tagger


import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }


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


case class NewsItem(link: String, title: String, img_link: String, body: String)


case class WordLink(word: String, score: Double)


object Tagger {
	private val tagToWordLinks = Map(
		"neuroscience"->Array(
			WordLink("affective", 0.060645),
			WordLink("cognitive", 0.050082),
			WordLink("neurology", 0.048206),
			WordLink("neural", 0.042843),
			WordLink("plasticity", 0.034417),
			WordLink("learning", 0.024417),
		),

		"javascript"->Array(
			WordLink("browsers", 0.057233),
			WordLink("css", 0.055593),
			WordLink("scripting", 0.052849),
			WordLink("widgets", 0.050512),
			WordLink("netscape", 0.045265),
			WordLink("click", 0.035265),
		),

		"literature"->Array(
			WordLink("literatures", 0.32747),
			WordLink("sangam", 0.234733),
			WordLink("rabbinic", 0.186105),
			WordLink("comparative", 0.153657),
			WordLink("sahitya", 0.140819),
			WordLink("educational", 0.130819),
		),

		"technology"->Array(
			WordLink("commercialization", 0.232641),
			WordLink("deloitte", 0.228937),
			WordLink("advancements", 0.227261),
			WordLink("birla", 0.205266),
			WordLink("incubator", 0.192419),
			WordLink("products", 0.092419),
			WordLink("program", 0.082419),
		),

		"family"->Array(
			WordLink("fabaceae", 1.071),
			WordLink("conidae", 0.997544),
			WordLink("gracillariidae", 0.994959),
			WordLink("leptodactylidae", 0.993837),
			WordLink("muricidae", 0.992335),
			WordLink("age", 0.792335),
			WordLink("kids", 0.692335)
		),

		"software"->Array(
			WordLink("antivirus", 0.435473),
			WordLink("gpl", 0.404681),
			WordLink("shareware", 0.390798),
			WordLink("gnu", 0.335025),
			WordLink("spyware", 0.332242),
			WordLink("tencent", 0.232),
		),
	)

	def tagNewsItem(newsItem: NewsItem): Array[String] = {
		// parse out all words & count occurrences
		val wordRegex = "(\\w+)".r

		val titleWords = wordRegex.findAllIn(newsItem.title.toLowerCase()).toList
		val bodyWords = wordRegex.findAllIn(newsItem.body.toLowerCase()).toList

		val allWords = titleWords ++ bodyWords
		val wordToOccurrences = allWords.groupBy(word=>word).mapValues(_.size)

		// score each potential tag, based on # of occurrences and it's relevance
		// i.e. scalar multiplication of occurrences-vector & scores-vector
		var tagScores = Array[(String, Double)]()
		for ((tag, wordLinks) <- tagToWordLinks) {
			var score = 0.0

			for (wordLink <- wordLinks) {
				val count = wordToOccurrences.getOrElse(wordLink.word, 0)
				score += wordLink.score * count
			}

			println(s"Score for $tag is $score")
			tagScores = tagScores :+ (tag, score)
		}

		// keep top up-to-5 non-zero tags
		val topTags = tagScores
			.filter(tagScore => tagScore._2 > 0)
			.sortWith(_._2 > _._2)
			.slice(0, 3)
			.map(tagScore => tagScore._1)

		topTags
	}
}


//#main-class
object AkkaQuickstart extends App {
	println("STARTING THE TAGGER")

	val item = NewsItem(
		"https://techcrunch.com/2018/04/08/tencent-and-education-startup-age-of-learning-bring-popular-english-learning-app-abcmouse-to-china/",
		"Tencent and education startup Age of Learning bring popular English-learning app ABCmouse to China",
		"https://techcrunch.com/wp-content/uploads/2018/04/tencent_abcmouse_header.jpg?w=586",
		"Tencent is teaming up with Los Angeles-based education company Age of Learning to launch an English education program for kids in China. ABCmouse, Age of Learning’s flagship product, has been localized and will be available as a website and an iOS and Android app in China, with Tencent handling product development, marketing, sales and customer support.\nThe new partnership extends Tencent’s involvement in ed-tech, which already includes a strategic investment in VIPKID, an online video tutoring platform that connects Chinese kids with English teachers and competes with QKids and Dada ABC. ABCmouse, on the other hand, uses videos, books and online activities like games, songs and stories to help kids study English.\nThe Chinese version of ABCmouse includes integration with Tencent’s ubiquitous messenger and online services platform WeChat, which now has more than one billion users, and its instant messaging service QQ, with 783 million monthly active users. This makes it easier for parents to sign up and pay for ABCmouse, because they can use their WeChat or QQ account and payment information. It also allows families to share kids’ English-learning progress on their news feeds or in chats. For example, Jerry Chen, Age of Learning’s president of Greater China, says parents can send video or audio recordings of their children practicing English to grandparents, who can then buy gift subscriptions with one click.\nThough you probably haven’t heard of it unless you have young kids or work with elementary school-age children, Age of Learning has built a significant presence in online education since it was founded in 2007, thanks mainly to the popularity of ABCmouse in schools, public libraries and Head Start programs. Two years ago, Age of Learning hit unicorn status after raising $150 million at a $1 billion valuation from Iconiq Capital.\nThe partnership lets ABCmouse tap into a major new audience. Chen says there are more than 110 million kids between the ages of three to eight in China and the online English language learning market there is “a several billion dollar market that’s growing rapidly.” He points to a recent study by Chinese research agency Yiou Intelligence that says total spending on online English learning programs for children will be 29.41 billion RMB, or about $4.67 billion, this year, and is projected to reach 79.17 billion, or $12.6 billion, by 2022.\nThe localization of ABCmouse will extend to the design of its eponymous cartoon rodent, who has a more stylized appearance in China. Lessons include animations featuring an English teacher and students in an international school classroom and begin with listening comprehension and speaking before moving onto phonics, reading and writing. Tencent-Age of Learning products will also include speech recognition tools to help kids hone their English pronunciation.\nIn an email, Jason Chen, Tencent’s general manager of online education, said that the company “reviewed several companies through an extensive research process, and it became clear that ABCmouse had the most engaging and effective online English self-learning curriculum and content for children. Age of Learning puts learning first, and that commitment to educational excellence made them a perfect fit for our online English language learning business.”"
	)

	val tags = Tagger.tagNewsItem(item)

	if (tags.length > 0) {
		println("Assigned some tags, need to send to REC-SYS")
		println(tags.mkString(", "))
	} else {
		println("Wasn't able to assign any tags, IGNORING this news item")
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
