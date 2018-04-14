package tasks


import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.math.{sqrt, Ordering}

import play.api.inject.{SimpleModule, _}

import akka.actor.ActorSystem
import javax.inject.Inject

import com.datastax.driver.core.{Cluster, ResultSet, Row}


class ReclusterTaskModule extends SimpleModule(bind[ReclusterTask].toSelf.eagerly())

class ReclusterTask @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {

	// Shorthand for cleaner code
	type Vec = Array[Double]

	// Array of user ids who are in same cluster
	type Cluster = Array[Int]


	// means(i) is centroid of vectors corresponding to clusters(i)
	case class KMeansResult(means: Array[Vec], clusters: Array[Cluster])


	object CassandraClient {
		private val cluster = Cluster.builder()
			.addContactPoint("localhost")
			.withPort(9042)
			.build()

		val session = cluster.connect()

		def getValueFromCassandraTable() = {
			val rs = session.execute("SELECT * FROM test01.clusters WHERE id = 0")
			val row = rs.one()
			(row.getInt("id"), row.getString("articles_json"))
		}

		def fetchUserTags() : mutable.Map[Int, Vec] = {
			val rs = session.execute("SELECT * FROM test01.user_tags")
			val rows = rs.all()

			// TODO: unhardcode tag names
			val idToTags = mutable.Map[Int, Vec]()
			for (row <- rows.asScala) {
				idToTags(row.getInt("id")) = Array(row.getInt("javascript"), row.getInt("literature"))
			}

			idToTags
		}

		def fetchArticles() : mutable.Map[Int, String] = {
			val rs = session.execute("SELECT * FROM test01.articles")
			val rows = rs.all()

			val idToBodyJson = mutable.Map[Int, String]()
			for (row <- rows.asScala) {
				idToBodyJson(row.getInt("id")) = row.getString("body_json")
			}

			idToBodyJson
		}

		def fetchArticleTags() : mutable.Map[Int, Vec] = {
			val rs = session.execute("SELECT * FROM test01.article_tags")
			val rows = rs.all()

			// TODO: unhardcode tag names
			val idToTags = mutable.Map[Int, Vec]()
			for (row <- rows.asScala) {
				idToTags(row.getInt("id")) = Array(row.getInt("javascript"), row.getInt("literature"))
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
				idToArticleJson(row.getInt("id")) = row.getString("body_json")
			}

			// maintain order
			ids.map(i => idToArticleJson(i))
		}

		// TODO: don't do this; store in a config table instead
		def getNextClusterId() : Int = {
			val rs = session.execute("SELECT MAX(id) as max_id FROM test01.clusters")
			val row = rs.one()

			row.getInt("max_id") + 1
		}

		def insertCluster(newId: Int, articlesJson: String, tags: Vec) : Unit = {
			session.execute("" +
				s"INSERT INTO test01.clusters (id, articles_json) " +
				s"VALUES ($newId, '$articlesJson')"
			)

			// TODO: unhardcode tag names
			session.execute("" +
				s"INSERT INTO test01.cluster_tags (id, javascript, literature) " +
				s"VALUES ($newId, ${tags(0)}, ${tags(1)})"
			)
		}

		def updateUser(userId: Int, newClusterId: Int) : Unit = {
			session.execute("" +
				s"UPDATE test01.users " +
				s"SET cluster_id = $newClusterId " +
				s"WHERE id = $userId"
			)
		}

	}

	object LinearAlgebra {
		// Scale vector down by its length
		def normalize(vector: Vec): Vec = {
			println("Normalizing " + vector.mkString(", "))
			val length = sqrt(vector.map(x => x * x).sum)
			println(s"len: $length\n\n")
			vector.map(_ / length)
		}

		// Distance between two vectors
		def distance(v1: Vec, v2: Vec): Double = {
			val dim = v1.length
			sqrt(
				Array
					.tabulate(dim) {
						i => (v2(i) - v1(i))
					}
					.map(x => x * x)
					.sum
			)
		}

		// Sum two vectors and write result into v1
		def addTo(v1: Vec, v2: Vec): Unit = {
			val dim = v1.length
			for (i <- 0 until dim) {
				v1(i) += v2(i)
			}
		}

		// Scalar product of 2 vectors
		def scalarProduct(v1: Vec, v2: Vec): Double = {
			val dim = v1.length
			Array
				.tabulate(dim) {
					i => v1(i) * v2(i)
				}
				.sum
		}
	}

	object KMeansClustering {

		// Run K-means & return list of means & list of clusters
		def run(k: Int, numSteps: Int, idToVector: mutable.Map[Int, Vec]): KMeansResult = {

			// Print params for debugging
			println("STARTING K-MEANS CLUSTERING ALGORITHM")
			println(s"    k: $k, steps: $numSteps, vectors:")
			for ((id, vector) <- idToVector.toSeq.sortBy(_._1)) {
				println(s"    $id -> " + vector.mkString("<", ",", ">") + ";")
			}

			/* Forgy initialization (choose random k vectors as means) */
			// val means = Array(idToVector(1), idToVector(2))
			val means = idToVector.values.toArray.slice(0, k)
			val clusters = Array.fill(k)(mutable.ArrayBuffer[Int]())
			println("@INIT:")
			println("    means: " + means.deep.mkString(" | "))
			println("    clusters: " + clusters.deep.mkString(" | "))

			for (step <- 0 until numSteps) {
				println(s"@STEP #$step:")

				/* Assignment step (find vectors nearest to means) */
				clusters.map(_.clear())
				for ((id, vector) <- idToVector) {
					var minIdx = 0
					var minDistance = LinearAlgebra.distance(vector, means(minIdx))

					for (idx <- 1 until k) {
						val mean = means(idx)
						val distance = LinearAlgebra.distance(vector, mean)
						if (distance < minDistance) {
							minIdx = idx
							minDistance = distance
						}
					}

					// append to nearest mean's cluster
					clusters(minIdx) += id
				}
				println("    clusters: " + clusters.deep.mkString(" | "))

				/* Update step (update each mean to be centroid of its cluster) */
				for (i <- 0 until k) {
					val cluster = clusters(i)
					val sumVector = Array.fill(k)(0.0)

					for (id <- cluster) {
						val vector = idToVector(id)
						LinearAlgebra.addTo(sumVector, vector)
					}

					val meanVector = sumVector.map(_ / cluster.length)
					means(i) = meanVector
				}
				println("    means: " + means.deep.mkString(" | "))

			}

			KMeansResult(means, clusters.map(_.toArray))
		}

	}

	// Class to keep track of top `limit` articles relevant to a cluster with `tags`;
	// Implemented as a light wrapper around scala's PriorityQueue;
	class ClusterTopArticles(clusterTags: Vec, limit: Int) {
		// Priority queue of (articleId, score) tupes, ordered based on score
		private val pq = new mutable.PriorityQueue[Tuple2[Int, Double]]()(Ordering.by[(Int, Double), Double](-_._2))

		// Adds the article to top news if it mathces well enough
		def addArticle(id: Int, articleTags: Vec): Unit = {
			val score = LinearAlgebra.scalarProduct(articleTags, clusterTags)
			pq.enqueue((id, score))

			// discard lowest scorers if limit exceeded
			if (pq.size > limit) {
				pq.dequeue()
			}
		}

		// Returns current list of best-matching articles & clears the list
		def getSortedArticles(): Array[Int] = {
			println(pq)
			pq
				.clone // to keep data in pq
				.dequeueAll
				.reverse // since we order by -score, need to reverse here to return best-first
				.map(_._1) // return only article ids
				.toArray
		}
	}

	// Schedule the task to recluster users, and assign the news per cluster
	actorSystem.scheduler.schedule(initialDelay = 1000.seconds, interval = 1.hour) {
		println("--- Starting the Recluster task ---")

		// 1. pull all user_tags into memory
		val idToUserTags = CassandraClient.fetchUserTags()

		// 2. normalize all
		val idToUserTagsNormalized = mutable.Map[Int, Vec]()
		for ((id, tags) <- idToUserTags) {
			idToUserTagsNormalized(id) = LinearAlgebra.normalize(tags)
		}

		// 3. run K-means clustering***:
		// - get cluster -> weights map
		// - get user_id -> cluster_id map
		val k = 2
		val numSteps = 10
		val kMeans = KMeansClustering.run(k, numSteps, idToUserTagsNormalized)

		// 4. save user_id -> cluster_id in a tmp table
		// NOT REQUIRED FOR SMALL DATASETS

		// 5. pull all articles & article_tags into memory
		// val idToArticleJson = CassandraClient.fetchArticles()
		val idToArticleTags = CassandraClient.fetchArticleTags()

		// 6. build cluster_id -> articles_json map
		val clusterTops = Array.tabulate(k) { i => new ClusterTopArticles(kMeans.means(i), 3) }
		for ((id, articleTags) <- idToArticleTags) {
			println(id + " - " + articleTags.mkString(","))
			for (clusterTop <- clusterTops) {
				clusterTop.addArticle(id, articleTags)
			}
		}
		println("Top articles by cluster:")
		for (clusterTop <- clusterTops) {
			val topArticles = clusterTop.getSortedArticles()
			println(topArticles.mkString(","))
		}

		// 7. insert into clusters & cluster_tags tables
		val clusterIdShift = CassandraClient.getNextClusterId()
		for (i <- 0 until k) {
			val clusterTop = clusterTops(i)
			val topArticleIds = clusterTop.getSortedArticles()

			// Many jsons of single articles
			val topArticleJsons = CassandraClient.fetchOrderedArticleJsons(topArticleIds)

			// One json representing all articles
			val topArticlesJson = topArticleJsons.mkString("[", ", ", "]")


			println(topArticlesJson)
			CassandraClient.insertCluster(clusterIdShift + i, topArticlesJson, kMeans.means(i))
		}

		// 8. wait & update user_id -> cluster_id
		for (i <- 0 until k) {
			val userIds = kMeans.clusters(i)
			val newClusterId = clusterIdShift + i

			for (userId <- userIds) {
				CassandraClient.updateUser(userId, newClusterId)
			}
		}
	}

}

