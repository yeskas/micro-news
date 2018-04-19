package consumers


import scala.collection.mutable
import scala.collection.JavaConverters._

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._


// Fields of interest in the incoming feedback jsons
case class Feedback(userId: Int, articleId: Int)


object FeedbackConsumer {
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

			println(s">>>>>>>>>>> $userId, >>>>>>>> $articleId")

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
				var userWeight = 0
				if (!row.isNull(tag)) {
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

	def handleMessage(message: String): Unit = {
		println("Received message: " + message)

		// Parse out the feedback object
		val feedbackJson = message;
		implicit val formats = DefaultFormats
		val json = parse(feedbackJson)
		val feedback = json.extract[Feedback]

		CassandraClient.addFeedback(feedback)

		println("Done with message\n\n\n")
	}
}
