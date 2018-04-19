package helpers


import scala.math.sqrt
import Types._


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
