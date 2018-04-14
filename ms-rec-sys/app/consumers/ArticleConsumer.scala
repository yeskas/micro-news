package consumers


import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.inject.{SimpleModule, _}

import akka.actor.ActorSystem
import javax.inject.Inject

import com.datastax.driver.core.{Cluster, ResultSet, Row}


class ArticleConsumerModule extends SimpleModule(bind[ArticleConsumer].toSelf.eagerly())

class ArticleConsumer @Inject() (actorSystem: ActorSystem) (implicit executionContext: ExecutionContext) {

	// Make it start @ app start
	actorSystem.scheduler.schedule(initialDelay = 1.second, interval = 2.seconds) {
		println("This is ARTICLE CONSUMER")
	}

}
