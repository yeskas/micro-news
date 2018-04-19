name := """ms-rec-sys"""
organization := "com.github.yeskas"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.1"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.github.yeskas.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.github.yeskas.binders._"

// Cassandra client
libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.4.0"

// Parsing JSON
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.3"

// Java Client for RabbitMQ
libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.2.0"

// RabbitMQ client based on Akka
val opRabbitVersion = "2.1.0"
libraryDependencies ++= Seq(
	"com.spingo" %% "op-rabbit-core"        % opRabbitVersion,
	"com.spingo" %% "op-rabbit-play-json"   % opRabbitVersion,
	"com.spingo" %% "op-rabbit-json4s"      % opRabbitVersion,
	"com.spingo" %% "op-rabbit-airbrake"    % opRabbitVersion,
	"com.spingo" %% "op-rabbit-akka-stream" % opRabbitVersion
)
