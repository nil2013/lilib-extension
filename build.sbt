import Dependencies._

ThisBuild / organization := "me.nsmr"
ThisBuild / scalaVersion := "2.13.0"
ThisBuild / version := "0.1.0-SNAPSHOT"

// lazy val root = (project in file(".")).dependsOn(common).
//   aggregate(analyzer, search, supremecourtdb)

lazy val common = (project in file("common")).
  settings(
    name := "common"
  )

lazy val analyzer = (project in file("lilib-analyzer")).
  settings(
    name := "lilib-analyzer",
    libraryDependencies ++= List(
      scalaTest % Test,
      lilib.core,
      logger.scalaLogging,
      logger.logback
    )
  ).dependsOn(common)

lazy val search = (project in file("lilib-search")).
  settings(
    name := "lilib-search",
    libraryDependencies ++= List(
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
  ).dependsOn(common)

lazy val supremecourtdb = (project in file("lilib-supremecourtdb")).
  settings(
    name := "lilib-supremecourtdb",
    libraryDependencies ++= List(
      scalaTest % Test,
      scalaXml,
      jsoup,
      itext.kernel,
      itext.asian,
      lilib.core
    )
  ).dependsOn(common)
