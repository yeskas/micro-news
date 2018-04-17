package tagger


import scala.io.Source
import scala.collection.mutable.ArrayBuffer

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._


// Classes to represent words related to tag words
case class Link(word: String, score: Double)
case class Tag(word: String, links: List[Link])


// Classes to represent incoming article jsons
case class ArticleSource(name: String, link: String, icon: String)
case class Article(id: Int, link: String, title: String, body: String, image: String, timestamp: String,
				   source: ArticleSource)


// Assigns tags to articles based on a specified config file
class Tagger(tagsFilePath: String) {
	private val allTags = parseTags()

	// Read entire tags file into memory, since it's small in size
	private def parseTags() : List[Tag] = {
		println(s"Parsing tags data from: $tagsFilePath")

		val source = Source.fromFile(tagsFilePath)
		val tagsJson = try source.mkString finally source.close()

		implicit val formats = DefaultFormats
		val json = parse(tagsJson)
		json.extract[List[Tag]]
	}

	// Scores each tag based on how often its linked-words occur * the weight of the linked word
	// Returns tags that score the most
	def tagArticle(article: Article): Array[String] = {
		// parse out all words & count occurrences
		val wordRegex = "(\\w+)".r

		val titleWords = wordRegex.findAllIn(article.title.toLowerCase()).toList
		val bodyWords = wordRegex.findAllIn(article.body.toLowerCase()).toList

		val allWords = titleWords ++ bodyWords
		val wordToOccurrences = allWords.groupBy(word=>word).mapValues(_.size)

		// score for tag = scalar multiplication (linked-words-occurrences-vector) * (linked-words-weights)
		val tagScores = new ArrayBuffer[(String, Double)]()
		for (tag <- allTags) {
			var score = 0.0

			for (link <- tag.links) {
				val count = wordToOccurrences.getOrElse(link.word, 0)
				score += link.score * count
			}

			println(s"Score for ${tag.word} is $score")
			tagScores += ((tag.word, score))
		}

		// keep top up-to-5 non-zero tags
		val topTags = tagScores
			.toArray
			.filter(tagScore => tagScore._2 > 0)
			.sortWith(_._2 > _._2)
			.slice(0, 5)
			.map(tagScore => tagScore._1)

		topTags
	}
}
