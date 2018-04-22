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
	private def getSortedTagNames() : Array[String] = {
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
	private def getAllTagValuesInRow(row: Row, colType: Int) : Vec = {
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




	//////// CRUD for users & user_tags tables ////////
	def getUserClusterId(userId: Int): Int = {
		val rs = session.execute(s"SELECT cluster_id FROM test01.users WHERE id = $userId")
		val row = rs.one()
		val clusterId = if (row == null) DEFAULT_CLUSTER_ID else row.getInt("cluster_id")
		clusterId
	}



	def updateUser(userId: Int, clusterId: Int) : Unit = {
		session.execute("" +
			s"UPDATE test01.users " +
			s"SET cluster_id = $clusterId " +
			s"WHERE id = $userId"
		)
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

	// Adds feedback about user's click on article to the db:
	// - adds user of first feedback
	// - increments user's tags by article's tags
	def addFeedback(userId: Int, articleId: Int) : Unit = {
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




	//////// CRUD for articles & article_tags tables ////////
	def fetchArticles() : mutable.Map[Int, String] = {
		val rs = session.execute("SELECT * FROM test01.articles")
		val rows = rs.all()

		val idToArticleJson = mutable.Map[Int, String]()
		for (row <- rows.asScala) {
			idToArticleJson(row.getInt("id")) = row.getString("article_json")
		}

		idToArticleJson
	}

	def insertArticle(id: Int, articleJson: String): Unit = {
		// got cash?
		session.execute("" +
			s"INSERT INTO test01.articles (id, article_json) " +
			s"VALUES ($id, $$$$$articleJson$$$$)"
		)
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

	def insertArticleTags(id: Int, tags: Array[String], tagValues: Array[Int]): Unit = {
		val tags_csv = tags.mkString(", ")
		val vals_csv = tagValues.mkString(", ")

		session.execute("" +
			s"INSERT INTO test01.article_tags (id, $tags_csv) " +
			s"VALUES ($id, $vals_csv)"
		)
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
			idToArticleJson(row.getInt("id")) = row.getString("article_json")
		}

		// maintain order
		ids.map(i => idToArticleJson(i))
	}




	//////// CRUD for clusters & cluster_tags tables ////////
	def getNextClusterId() : Int = {
		val rs = session.execute("SELECT MAX(id) as max_id FROM test01.clusters")
		val row = rs.one()

		row.getInt("max_id") + 1
	}

	def fetchCluster(id: Int) : (Int, String, String) = {
		val rs = session.execute(s"SELECT * FROM test01.clusters where id = $id")
		val row = rs.one()

		(id, row.getString("articles_json"), row.getString("scores_json"))
	}

	def insertCluster(id: Int, articlesJson: String, scoresJson: String) : Unit = {
		session.execute("" +
			s"INSERT INTO test01.clusters (id, articles_json, scores_json) " +
			s"VALUES ($id, $$$$$articlesJson$$$$, '$scoresJson')"
		)
	}

	def updateCluster(id: Int, articlesJson: String, scoresJson: String): Unit = {
		session.execute("" +
			s"UPDATE test01.clusters " +
			s"SET articles_json = $$$$$articlesJson$$$$, scores_json = '$scoresJson' " +
			s"WHERE id = $id"
		)
	}

	def fetchClusterTags(tags: List[String]) : mutable.Map[Int, Vec] = {
		val rs = session.execute(s"SELECT id, ${tags.mkString(", ")} FROM test01.cluster_tags")
		val rows = rs.all()

		val idToTags = mutable.Map[Int, Vec]()
		for (row <- rows.asScala) {
			idToTags(row.getInt("id")) = tags.map(row.getDouble(_)).toArray
		}

		idToTags
	}

	def insertClusterTags(id: Int, tagValues: Vec): Unit = {
		session.execute("" +
			s"INSERT INTO test01.cluster_tags (id, ${sortedTagNames.mkString(", ")}) " +
			s"VALUES ($id, ${tagValues.mkString(", ")})"
		)
	}
}
