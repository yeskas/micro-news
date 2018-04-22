package helpers


import scala.collection.mutable
import scala.collection.JavaConverters._
import com.datastax.driver.core.{Cluster, ResultSet, Row}

// TODO: DONT PARSE HERE
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import Configs._
import Types._


// TODO: SEPARATE ARTICLE LOGIC FROM DB LOCIC


object CassandraClient {
	private val COL_TYPE_INT = 0
	private val COL_TYPE_DOUBLE = 1
	private val COL_TYPE_STRING = 2

	private val cluster = Cluster
		.builder()
		.addContactPoint("localhost")
		.withPort(9042)
		.build()

	private val session = cluster.connect()

	private val sortedTagNames = getSortedTagNames()

	// Returns (same) tag names used in:
	// - user_tags
	// - article_tags
	// - cluster_tags
	def getSortedTagNames() : Array[String] = {
		val rs = session.execute("SELECT * FROM test01.cluster_tags WHERE id = 0")

		val allTagNames = mutable.ArrayBuffer[String]()
		for (columnDef <- rs.getColumnDefinitions.asList().asScala) {
			val column = columnDef.getName()
			if (column != "id") {
				allTagNames += column
			}
		}

		allTagNames.sorted.toArray
	}

	// Gets ordered (sorted by tag name) values of tags in a row of any of:
	// - user_tags
	// - article_tags
	// - cluster_tags
	def getAllTagValuesInRow(row: Row, colType: Int) : Vec = {
		val result = mutable.ArrayBuffer[Double]()
		for (tag <- sortedTagNames) {
			if (row.isNull(tag)) {
				result += 0
			} else if (colType == COL_TYPE_INT) {
				result += row.getInt(tag)
			} else {
				result += row.getDouble(tag)
			}
		}
		result.toArray
	}

	def getValueFromCassandraTable() = {
		val rs = session.execute("SELECT * FROM test01.clusters WHERE id = 0")
		val row = rs.one()
		(row.getInt("id"), row.getString("articles_json"))
	}

	def fetchUserTags() : mutable.Map[Int, Vec] = {
		val rs = session.execute("SELECT * FROM test01.user_tags")
		val rows = rs.all()

		val idToTags = mutable.Map[Int, Vec]()
		for (row <- rows.asScala) {
			idToTags(row.getInt("id")) = getAllTagValuesInRow(row, COL_TYPE_INT)
		}

		idToTags
	}

	def fetchArticles() : mutable.Map[Int, String] = {
		val rs = session.execute("SELECT * FROM test01.articles")
		val rows = rs.all()

		val idToBodyJson = mutable.Map[Int, String]()
		for (row <- rows.asScala) {
			idToBodyJson(row.getInt("id")) = row.getString("article_json")
		}

		idToBodyJson
	}

	def fetchArticleTags() : mutable.Map[Int, Vec] = {
		val rs = session.execute("SELECT * FROM test01.article_tags")
		val rows = rs.all()

		val idToTags = mutable.Map[Int, Vec]()
		for (row <- rows.asScala) {
			idToTags(row.getInt("id")) = getAllTagValuesInRow(row, COL_TYPE_INT)
		}

		idToTags
	}

	// Fetch article jsons from db in the order specified
	def fetchOrderedArticleJsons(ids: Array[Int]) : Array[String] = {
		val rs = session.execute("" +
			s"SELECT * FROM test01.articles " +
			s"WHERE id in (${ids.mkString(", ")})"
		)
		val rows = rs.all()

		val idToArticleJson = mutable.Map[Int, String]()
		for (row <- rows.asScala) {
			// TODO: add article id to the article json here or in the caller
			idToArticleJson(row.getInt("id")) = row.getString("article_json")
		}

		// maintain order
		ids.map(i => idToArticleJson(i))
	}

	def fetchCluster(id: Int) : (Int, String, String) = {
		val rs = session.execute(s"SELECT * FROM test01.clusters where id = $id")
		val row = rs.one()

		(id, row.getString("articles_json"), row.getString("scores_json"))
	}

	// TODO: don't do this; store in a config table instead
	def getNextClusterId() : Int = {
		val rs = session.execute("SELECT MAX(id) as max_id FROM test01.clusters")
		val row = rs.one()

		row.getInt("max_id") + 1
	}

	def insertCluster(newId: Int, articlesJson: String, topScoresJson: String, tags: Vec) : Unit = {
		session.execute("" +
			s"INSERT INTO test01.clusters (id, articles_json, scores_json) " +
			s"VALUES ($newId, $$$$$articlesJson$$$$, '$topScoresJson')"
		)

		session.execute("" +
			s"INSERT INTO test01.cluster_tags (id, ${sortedTagNames.mkString(", ")}) " +
			s"VALUES ($newId, ${tags.mkString(", ")})"
		)
	}

	def fetchSomeClusterTags(tags: List[String]) : mutable.Map[Int, Vec] = {
		val rs = session.execute(s"SELECT id, ${tags.mkString(", ")} FROM test01.cluster_tags")
		val rows = rs.all()

		val idToTags = mutable.Map[Int, Vec]()
		for (row <- rows.asScala) {
			idToTags(row.getInt("id")) = tags.map(row.getDouble(_)).toArray
		}

		idToTags
	}

	def updateUser(userId: Int, newClusterId: Int) : Unit = {
		session.execute("" +
			s"UPDATE test01.users " +
			s"SET cluster_id = $newClusterId " +
			s"WHERE id = $userId"
		)
	}

	def insertArticle(id: Int, articleJson: String): Unit = {
		// got cash?
		session.execute("" +
			s"INSERT INTO test01.articles (id, article_json) " +
			s"VALUES ($id, $$$$$articleJson$$$$)"
		)
	}

	def insertArticleTags(id: Int, tags: Array[String], tagValues: Array[Int]): Unit = {
		val tags_csv = tags.mkString(", ")
		val vals_csv = tagValues.mkString(", ")

		session.execute("" +
			s"INSERT INTO test01.article_tags (id, $tags_csv) " +
			s"VALUES ($id, $vals_csv)"
		)
	}

	def fetchClusterArticlesJson(id: Int) : String = {
		val rs = session.execute(s"SELECT * FROM test01.clusters where id = $id")
		rs.one().getString("articles_json")
	}

	def updateCluster(id: Int, articlesJson: String, scoresJson: String): Unit = {
		session.execute("" +
			s"UPDATE test01.clusters " +
			s"SET articles_json = $$$$$articlesJson$$$$, scores_json = '$scoresJson' " +
			s"WHERE id = $id"
		)
	}
}
