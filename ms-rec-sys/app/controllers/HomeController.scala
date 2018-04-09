package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class NewsItem(title: String, body: String, order: Int)


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

	implicit val newsItemWrites: Writes[NewsItem] = (
		(JsPath \ "title").write[String] and
		(JsPath \ "body").write[String] and
		(JsPath \ "order").write[Int]
	)(unlift(NewsItem.unapply))

	/**
	* Create an Action to render an HTML page.
	*
	* The configuration in the `routes` file means that this method
	* will be called when the application receives a `GET` request with
	* a path of `/`.
	*/
	def index() = Action { implicit request: Request[AnyContent] =>
		val newsItem = NewsItem("MU beat MC", "This Saturday MU beat MC, which delayed MC's championship by at least 1 week", 55)
		Ok(Json.toJson(newsItem))
	}
}
