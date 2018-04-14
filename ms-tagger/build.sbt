name := "ms-tagger"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-testkit" % akkaVersion,
	"org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// Parsing JSON
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.3"

// Java Client for RabbitMQ
libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.2.0"
