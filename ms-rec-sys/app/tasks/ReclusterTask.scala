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

		val result = CassandraClient.getValueFromCassandraTable()
		println(result)

	}

}
