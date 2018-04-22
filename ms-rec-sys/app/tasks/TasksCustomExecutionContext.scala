package tasks


import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext


// Custom ctx for TasksModule
class TasksCustomExecutionContext @Inject() (actorSystem: ActorSystem)
	extends CustomExecutionContext(actorSystem, "tasks-dispatcher")
