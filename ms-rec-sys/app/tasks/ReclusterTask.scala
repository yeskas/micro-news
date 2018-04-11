package tasks


import javax.inject.Inject

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import com.datastax.driver.core.{Cluster, ResultSet, Row}


class ReclusterTask @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {

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

	// Schedule the task to recluster users, and assign the news per cluster
	actorSystem.scheduler.schedule(initialDelay = 1.second, interval = 1.hour) {
		println("--- Starting the Recluster task ---")

		// 1. pull all user_tags into memory

		// 2. normalize all

		// 3. run K-mean clustering:
		// - get cluster -> weights map
		// - get user_id -> cluster_id map

		// 4. save user_id -> cluster_id in a tmp table

		// 5. pull all articles & article_tags into memory

		// 6. build cluster_id -> articles_json map

		// 7. insert into clusters table

		// 8. insert into cluster_tags table

		// 9. wait & update user_id -> cluster_id

		val result = CassandraClient.getValueFromCassandraTable()
		println(result)

	}

}
