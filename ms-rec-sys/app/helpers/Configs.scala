package helpers


// Project constants
object Configs {
	// K in the K-Means algorithm
	val CLUSTERING_K = 4

	// # of steps in the K-Means algorithm
	val CLUSTERING_STEPS = 20

	// How many best articles to add to each cluster
	val CLUSTERING_KEEP_TOP = 40

	// The cluster with latest news & no tags
	val DEFAULT_CLUSTER_ID = 0

	// Expire articles so that users don't see stale news
	val ARTICLE_TTL_IN_DAYS = 3
}
