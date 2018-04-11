package controllers


// TODO: don't use "_" in imports
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import com.datastax.driver.core.{Cluster, ResultSet, Row}


case class NewsItem(title: String, body: String, order: Int)


/**
 * This controller creates `Action`s for different recommendation requests
 */
@Singleton
class RecommendationController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

	implicit val newsItemWrites: Writes[NewsItem] = (
		(JsPath \ "title").write[String] and
		(JsPath \ "body").write[String] and
		(JsPath \ "order").write[Int]
	)(unlift(NewsItem.unapply))

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
	}

	/**
	* Create an Action to return the JSON of recommended news for the specified user_id
	*/
	def index(userId: Int) = Action { implicit request: Request[AnyContent] =>
		val newsItem = NewsItem("MU beat MC", "This Saturday MU beat MC, which delayed MC's championship by at least 1 week", 55)

		val result = CassandraClient.getValueFromCassandraTable()
		println(result)

		println("----")
		println(s"Pulling rec for user #$userId")

		// Ok(Json.toJson(newsItem))
		Ok(Json.parse(result._2))
	}
}