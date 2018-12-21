package me.nsmr
package lilib
package sample

import akka.actor._
import me.nsmr.lilib
import lilib.supremecourtdb.data.{ SupremeCourtDbDataProvider, FileSupremeCourtDbDataProvider }
import lilib.search.{ PrecedentIndex, PrecedentIndexBuilder, ConcurrentPrecedentIndexBuilder }
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger

object Sample {

}

class Sample {
  implicit lazy val sys = ActorSystem("LIlib-Sample")
  lazy val logger = Logger[Sample]

  val settings = sys.settings.config.root
  logger.debug(settings.toString)

  def getProvider: SupremeCourtDbDataProvider[String] = new FileSupremeCourtDbDataProvider

  def getIndexBuilder: ConcurrentPrecedentIndexBuilder[String] = PrecedentIndexBuilder.concurrentBuilder[String]
}
