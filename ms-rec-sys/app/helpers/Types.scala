package helpers


object Types {
	// Shorthand for cleaner code
	type Vec = Array[Double]

	// Array of ids that are in the same cluster
	type IdsInCluster = Array[Int]

	// TODO: PUT INTO ArticleConsumer?
	// Classes to represent incoming article jsons
	case class ArticleSource(name: String, link: String, icon: String)
	case class Article(id: Int, link: String, title: String, body: String, image: String, timestamp: String,
					   source: ArticleSource, tags: List[String])
}
