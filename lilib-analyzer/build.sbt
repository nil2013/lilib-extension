import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "me.nsmr",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "lilib-analyzer",
    libraryDependencies ++= List (
      scalaTest % Test,
      lilib.core,
      logger.scalaLogging,
      logger.logback
    )
  )
