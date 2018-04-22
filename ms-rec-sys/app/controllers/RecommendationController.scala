package controllers


import scala.collection.mutable

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.datastax.driver.core.{Cluster, ResultSet, Row}

import helpers.CassandraClient


/**
 * This controller creates `Action`s for different recommendation requests
 */
@Singleton
class RecommendationController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
	/**
	* Create an Action to return the JSON of recommended news for the specified user_id
	*/
	def index(userId: Int) = Action { implicit request: Request[AnyContent] =>
		println(s"Pulling recommendation for user #$userId")

		val clusterId = CassandraClient.getUserClusterId(userId)
		val articlesJson = CassandraClient.fetchCluster(clusterId)._2

		Ok(Json.parse(articlesJson))
	}
}
