package consumers


import scala.collection.mutable
import scala.collection.JavaConverters._

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import helpers.CassandraClient


// Fields of interest in the incoming feedback jsons
case class Feedback(userId: Int, articleId: Int)


// RabbitMQ message handler for user feedback
object FeedbackConsumer {
	def handleMessage(feedbackJson: String): Unit = {
		// Parse out the feedback object
		implicit val formats = DefaultFormats
		val feedback = parse(feedbackJson).extract[Feedback]

		// Add to user* tables
		CassandraClient.addFeedback(feedback.userId, feedback.articleId)
	}
}
