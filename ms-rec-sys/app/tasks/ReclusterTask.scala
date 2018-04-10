package tasks


import javax.inject.Inject

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class ReclusterTask @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {

	// Schedule the task to recluster users, and assign the news per cluster
	actorSystem.scheduler.schedule(initialDelay = 1.second, interval = 1.second) {
		println("Starting the Recluster task")
		// TODO: implement reclustering here
	}

}
