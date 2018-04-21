package helpers


// Project-wise constants
object Configs {
	// K in the K-Means algorithm
	val CLUSTERING_K = 2

	// # of steps in the K-Means algorithm
	val CLUSTERING_STEPS = 10

	// How many best articles to add to each cluster
	val CLUSTERING_KEEP_TOP = 3

	// The cluster with latest news & no tags
	val DEFAULT_CLUSTER_ID = 0
}
