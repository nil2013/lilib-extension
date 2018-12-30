import Dependencies._

lazy val root = (project in file(".")).
  aggregate(analyzer, search, supremecourtdb)

lazy val analyzer = (project in file("lilib-analyzer")).
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

lazy val search = (project in file("lilib-search")).
  settings(
    inThisBuild(List(
      organization := "me.nsmr",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "lilib-search",
    libraryDependencies ++= List (
      scalaTest % Test,
      scalaXml,
      jsoup,
      itext.kernel,
      itext.asian,
      lilib.core,
      logger.scalaLogging,
      logger.logback,
      akkaActor
    )
  )

lazy val supremecourtdb = (project in file("lilib-supremecourtdb")).
  settings(
    inThisBuild(List(
      organization := "me.nsmr",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "lilib-supremecourtdb",
    libraryDependencies ++= List (
      scalaTest % Test,
      scalaXml,
      jsoup,
      itext.kernel,
      itext.asian,
      lilib.core
    )
  )
