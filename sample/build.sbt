import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "me.nsmr",
      scalaVersion := "2.12.5",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "sample",
    libraryDependencies ++= List(
      scalaTest % Test,
      lilib.core,
      lilib.analyzer,
      lilib.search,
      lilib.supremecourtdb,
      logger.scalaLogging,
      logger.logback,
      akkaActor
    )
  )
