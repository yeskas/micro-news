package consumers


import scala.collection.mutable

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._


// Fields of interest in the incoming article jsons
case class Article(id: Int, tags: List[String])


object ArticleConsumer {
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

	def handleMessage(message: String): Unit = {
		println("Received message: " + message)

		// Parse out the article object
		val articleJson = message
		implicit val formats = DefaultFormats
		val json = parse(articleJson)
		val article = json.extract[Article]

		CassandraClient.addArticle(article.id, article.tags, articleJson)

		println("Done with message\n\n\n")
	}
}
