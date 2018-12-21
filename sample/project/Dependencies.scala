import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  object logger {
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
    lazy val logback      = "ch.qos.logback" % "logback-classic" % "1.2.3"
  }

  object lilib {
    lazy val core           = "me.nsmr" %% "lilib" % "0.1.0-SNAPSHOT"
    lazy val analyzer       = "me.nsmr" %% "lilib-analyzer" % "0.1.0-SNAPSHOT"
    lazy val search         = "me.nsmr" %% "lilib-search" % "0.1.0-SNAPSHOT"
    lazy val supremecourtdb = "me.nsmr" %% "lilib-supremecourtdb" % "0.1.0-SNAPSHOT"
  }
  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.5.18"
}
