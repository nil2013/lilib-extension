package me.nsmr
package lilib.search

import akka.actor.{ ActorSystem, PoisonPill }
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import me.nsmr.lilib.core.{ Precedent }
import me.nsmr.lilib.search.actors._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object PrecedentIndexBuilder {
  def concurrentBuilder[K](implicit sys: ActorSystem): ConcurrentPrecedentIndexBuilder[K] = {
    new ConcurrentPrecedentIndexBuilder[K]
  }

  def concurrentlyBuilding[K, T](body: ConcurrentPrecedentIndexBuilder[K] => T)(implicit sys: ActorSystem): T = {
    val builder = concurrentBuilder
    try {
      body(concurrentBuilder)
    } finally {
      builder.close
    }
  }
}

trait PrecedentIndexBuilder[K] {
  def add(key: K, value: Precedent): Unit
  def add(list: Iterable[(K, Precedent)]): Unit
  def add(it: Iterator[(K, Precedent)]): Unit
  def build: PrecedentIndex[K]
}

final class ConcurrentPrecedentIndexBuilder[K](implicit private val system: ActorSystem) extends PrecedentIndexBuilder[K] {

  private[this] val actor = MainActor.getActorRef[K]

  def add(key: K, value: Precedent): Unit = {
    actor ! MainActor.Messages.AddItem(key, value)
  }

  def add(list: Iterable[(K, Precedent)]): Unit = {
    this.add(list.iterator)
  }

  def add(it: Iterator[(K, Precedent)]): Unit = {
    actor ! MainActor.Messages.AddItemList(it)
  }

  def build: PrecedentIndex[K] = {
    val future = buildInFuture
    await(future.isCompleted, "Building PrecedentIndex...")
    future.value match {
      case Some(Success(index)) => index
      case Some(Failure(e)) => throw e
      case None => throw new Exception("Something went wrong while building PrecedentIndex!")
    }
  }

  def buildInFuture: Future[PrecedentIndex[K]] = {
    implicit val timeout = Timeout(20 minutes)
    actor ? MainActor.Messages.RequestResult map {
      case MainActor.Messages.RespondIndexes(
        indexByDates: Map[_, Set[K]],
        indexByCaseNumbers: Map[_, Set[K]],
        indexByCourts: Map[_, Set[K]]
      ) => {
        new PrecedentIndex[K](indexByDates, indexByCaseNumbers, indexByCourts)
      }
    }
  }

  def close(): Unit = actor ! PoisonPill
}
