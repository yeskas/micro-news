package consumers


import scala.collection.mutable

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import helpers.Types._
import helpers.CassandraClient


object ArticleConsumer {
	def handleMessage(message: String): Unit = {
		println("Received message: " + message)

		// Parse out the article object
		val articleJson = message
		implicit val formats = DefaultFormats
		val json = parse(articleJson)
		val article = json.extract[Article]


		CassandraClient.addArticle(article.id, article.tags, articleJson, article)

		println("Done with message\n\n\n")
	}
}
