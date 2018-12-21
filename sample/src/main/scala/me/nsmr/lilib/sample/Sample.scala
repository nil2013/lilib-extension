package me.nsmr
package lilib
package sample

import akka.actor._
import me.nsmr.lilib
import lilib.core.Precedent
import lilib.supremecourtdb.data.{ SupremeCourtDbDataProvider, FileSupremeCourtDbDataProvider }
import lilib.search.{ PrecedentIndex, PrecedentIndexBuilder, ConcurrentPrecedentIndexBuilder }
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.Logger

object Sample {

}

class Sample {
  implicit lazy val sys = ActorSystem("LIlib-Sample")
  lazy val logger = Logger[Sample]

  def createProvider: SupremeCourtDbDataProvider[String] = new FileSupremeCourtDbDataProvider

  def createIndexBuilder: ConcurrentPrecedentIndexBuilder[String] = PrecedentIndexBuilder.concurrentBuilder[String]

  def getPrecedentList: Iterator[(String, Precedent)] = createProvider.iterator.map { data => (data.id, data.asPrecedent) }

  def getPrecedentIndex(size: Int): PrecedentIndex[String] = {
    using(createIndexBuilder) { builder =>
      builder.add(getPrecedentList.take(size))
      builder.build
    }
  }

  def getPrecedentIndex: PrecedentIndex[String] = {
    PrecedentIndexBuilder.concurrentlyBuilding { builder: ConcurrentPrecedentIndexBuilder[String] =>
      builder.add(getPrecedentList)
    }
  }
}
