package tasks


import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.math.Ordering

import play.api.inject.{SimpleModule, _}
import akka.actor.ActorSystem
import javax.inject.Inject

import helpers.Types._
import helpers.LinearAlgebra
import helpers.{KMeansResult, KMeansClustering}
import helpers.CassandraClient


class ReclusterTaskModule extends SimpleModule(bind[ReclusterTask].toSelf.eagerly())

class ReclusterTask @Inject() (actorSystemNI: ActorSystem) (implicit executionContext: ExecutionContext) {

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


	// Step #1 of recluster task: groups all existing users into clusters
	def stepClusterUsers(): KMeansResult = {
		// 1. pull all user_tags into memory
		val idToUserTags = CassandraClient.fetchUserTags()

		// 2. normalize all
		val idToUserTagsNormalized = mutable.Map[Int, Vec]()
		for ((id, tags) <- idToUserTags) {
			idToUserTagsNormalized(id) = LinearAlgebra.normalize(tags)
		}

		// 3. run K-means clustering:
		// - get cluster -> weights map
		// - get user_id -> cluster_id map
		// TODO: turn these 2 vals into CONFIG PARAMS
		val k = 2
		val numSteps = 10
		val kMeans = KMeansClustering.run(k, numSteps, idToUserTagsNormalized)

		// 4. save user_id -> cluster_id in a tmp table
		// NOT REQUIRED FOR SMALL DATASETS

		kMeans
	}

	// Step #2 of recluster task: given the clusters from step #1, collects best-matching articles for each cluster
	def stepAssignNewsToClusters(kMeans: KMeansResult): Unit = {
		val k = 2

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




	import akka.actor.{ ActorRef, ActorSystem, Props }
	import com.spingo.op_rabbit.{ Directives, Message, Publisher, Queue, RabbitControl, RecoveryStrategy, Subscription, SubscriptionRef }
	import scala.concurrent.ExecutionContext

	import consumers.ArticleConsumer
	import consumers.FeedbackConsumer



	implicit val actorSystem = ActorSystem("demo")
	val rabbitControl = actorSystem.actorOf(Props(new RabbitControl))
	implicit val recoveryStrategy = RecoveryStrategy.nack(false)
	//import ExecutionContext.Implicits.global


	val demoQueue = Queue("demo", durable = true, autoDelete = false)
	val articleQueue = Queue("tagger:rec-sys:articles", durable = true, autoDelete = false)
	val feedbackQueue = Queue("gateway:rec-sys:feedback", durable = true, autoDelete = false)


	def addArticleConsumerToRabbitControl(rabbitControl : ActorRef, suffix : String) : SubscriptionRef  = {
		val result = Subscription.run(rabbitControl) {
			import Directives._
			channel(qos=1) {
				consume(demoQueue) {
					body(as[String]) { data =>
						println(s"received <<<$suffix>>>: ${data}")
						ack
					}
				}
			}
		}

		result
	}
	def registerArticleConsumer(rabbitControl: ActorRef) : SubscriptionRef  = {
		val subscription = Subscription.run(rabbitControl) {
			import Directives._
			channel(qos=1) {
				consume(articleQueue) {
					body(as[String]) { data =>
						println(s"received ARTICLE: : ${data}")
						ArticleConsumer.handleMessage(data)
						ack
					}
				}
			}
		}

		subscription
	}
	def registerFeedbackConsumer(rabbitControl: ActorRef) : SubscriptionRef  = {
		val subscription = Subscription.run(rabbitControl) {
			import Directives._
			channel(qos=1) {
				consume(feedbackQueue) {
					body(as[String]) { data =>
						println(s"received FEEDBACK: : ${data}")
						FeedbackConsumer.handleMessage(data)
						ack
					}
				}
			}
		}

		subscription
	}


	var subscription = addArticleConsumerToRabbitControl(rabbitControl, "INITIAL")
	var articleConsumer = registerArticleConsumer(rabbitControl)
	var feedbackConsumer = registerFeedbackConsumer(rabbitControl)




	// Schedule the task to:
	// - 1. recluster users, and
	// - 2. assign the news per cluster
	actorSystemNI.scheduler.schedule(initialDelay = 1.second, interval = 1.day) {
		println("--- Starting the Recluster task ---")

		feedbackConsumer.close()
		feedbackConsumer.closed.foreach { _ =>
			val kMeans = stepClusterUsers()
			feedbackConsumer = registerFeedbackConsumer(rabbitControl)

			articleConsumer.close()
			articleConsumer.closed.foreach { _ =>
				stepAssignNewsToClusters(kMeans)
				articleConsumer = registerArticleConsumer(rabbitControl)
			}
		}
	}

}

