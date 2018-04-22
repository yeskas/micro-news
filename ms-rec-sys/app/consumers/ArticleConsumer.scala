package consumers


import scala.collection.mutable

import com.datastax.driver.core.{Cluster, ResultSet, Row}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import helpers.Configs._
import helpers.Types._
import helpers.CassandraClient


object ArticleConsumer {

	// Converts list of articles to json string
	private def articlesToJson(articles: List[Article]): String = {
		articles
			.map(a =>
				compact(render(
					("id" -> a.id) ~
					("link" -> a.link) ~
					("title" -> a.title) ~
					("body" -> a.body) ~
					("image" -> a.image) ~
					("timestamp" -> a.timestamp) ~
					("source" ->
						("name" -> a.source.name) ~
						("link" -> a.source.link) ~
						("icon" -> a.source.icon)
					) ~
					("tags" -> a.tags)
				))
			)
			.mkString("[", ", ", "]")
	}

	private def addArticleToDB(article: Article, articleJson: String) : Unit = {
		// For json parsing
		implicit val formats = DefaultFormats

		// Insert article itself
		CassandraClient.insertArticle(article.id, articleJson)

		// Insert weight of 1 to each of its present tags
		CassandraClient.insertArticleTags(article.id, article.tags.toArray, Array.fill(article.tags.length)(1))

		// Prepend to default cluster
		var articlesJson = CassandraClient.fetchCluster(DEFAULT_CLUSTER_ID)._2
		var articles = parse(articlesJson).extract[List[Article]]
		articles = article +: articles.dropRight(1)
		articlesJson = articlesToJson(articles)
		CassandraClient.updateCluster(DEFAULT_CLUSTER_ID, articlesJson, "")

		// Add to non-default clusters where it's in top list of matching articles
		val clusterIdToTagValues = CassandraClient.fetchSomeClusterTags(article.tags)
		for ((clusterId, tagValues) <- clusterIdToTagValues) {
			if (clusterId != DEFAULT_CLUSTER_ID) {
				// score of the new article in this cluster = sum of its matching tag weights
				val score = tagValues.sum
				val cluster = CassandraClient.fetchCluster(clusterId)

				val articles = parse(cluster._2).extract[List[Article]].to[mutable.ArrayBuffer]
				val scores = parse(cluster._3).extract[List[Double]].to[mutable.ArrayBuffer]

				// Insert into list of ordered articles (& corresponding scores) if better than the current worst
				if (scores.last <= score) {
					articles.trimEnd(1)
					scores.trimEnd(1)

					// Insert into sorted ArrayBuffer
					var insertIdx = scores.length
					for (idx <- (scores.length - 1) to 0 by -1) {
						if (score >= scores(idx)) {
							insertIdx = idx
						}
					}
					articles.insert(insertIdx, article)
					scores.insert(insertIdx, score)

					val articlesJson = articlesToJson(articles.toList)
					val scoresJson = scores.mkString("[", ", ", "]")
					CassandraClient.updateCluster(clusterId, articlesJson, scoresJson)
				}
			}
		}
	}

	def handleMessage(articleJson: String): Unit = {
		// Parse out the article object
		implicit val formats = DefaultFormats
		val article = parse(articleJson).extract[Article]

		// Add to article* & clusters tables
		addArticleToDB(article, articleJson)
	}
}
