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
