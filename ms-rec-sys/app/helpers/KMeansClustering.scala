package helpers


import scala.collection.mutable
import Types._


// means(i) is centroid of vectors corresponding to users in clusters(i)
case class KMeansResult(means: Array[Vec], clusters: Array[IdsInCluster])


object KMeansClustering {

	// Run K-means & return list of means & list of clusters
	def run(k: Int, numSteps: Int, idToVector: mutable.Map[Int, Vec]): KMeansResult = {
		// Identify dimension of vectors
		val dim = idToVector.head._2.length

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
				val sumVector = Array.fill(dim)(0.0)

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
