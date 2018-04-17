name := "ms-tagger"

version := "1.0"

scalaVersion := "2.12.2"

// Parsing JSON
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.3"

// Java Client for RabbitMQ
libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.2.0"
