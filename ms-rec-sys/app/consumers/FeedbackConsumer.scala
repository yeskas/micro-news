package consumers


import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.inject.{SimpleModule, _}

import akka.actor.ActorSystem
import javax.inject.Inject

import com.datastax.driver.core.{Cluster, ResultSet, Row}


class FeedbackConsumerModule extends SimpleModule(bind[FeedbackConsumer].toSelf.eagerly())

class FeedbackConsumer @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {

	// Make it start @ app start
	actorSystem.scheduler.schedule(initialDelay = 100.seconds, interval = 100.days) {
		println("This is FEEDBACK CONSUMER")
	}

}
