package me.nsmr
package lilib.search
package actors

import akka.actor.{ Actor, ActorRef, Props, PoisonPill }
import java.time.LocalDate
import me.nsmr.lilib.core._

object BuildIndexActors {
  object props {
    def date[K]: Props = Props[BuildDateIndexActor[K]]
    def court[K]: Props = Props[BuildCourtIndexActor[K]]
    def caseNumber[K]: Props = Props[BuildCaseNumberIndexActor[K]]
  }
  object Messages {
    case class AddDateIndex[K](date: LocalDate, key: K)
    case class AddCourtIndex[K](court: Court, key: K)
    case class AddCaseNumberIndex[K](caseNumber: CaseNumber, key: K)
    case class SetTotalCount(n: Int)
    case class RespondDateIndex[K](map: Map[LocalDate, Set[K]], total: Int = -1)
    case class RespondCourtIndex[K](map: Map[Court, Set[K]], total: Int = -1)
    case class RespondCaseNumberIndex[K](map: Map[CaseNumber, Set[K]], total: Int = -1)
  }
}

class BuildDateIndexActor[K] extends Actor {
  import BuildIndexActors._
  private[this] var index: Map[LocalDate, Set[K]] = Map.empty.withDefaultValue(Set.empty)
  private[this] var total: Int = -1
  private[this] var finished: Int = 0

  def receive = ({
    case Messages.AddDateIndex(date, key: K) => {
      this.index = this.index + (date -> (this.index(date) + key))
      this.finished += 1
    }
    case Messages.SetTotalCount(cnt) => this.total = cnt
  }: Any --> Unit).andThen { _ =>
    printinf(s"total: ${total} / finished: ${finished}")
    if(total > 0 && total == finished) {
      context.parent ! Messages.RespondDateIndex(index, finished)
      self ! PoisonPill
    }
  }
}

class BuildCourtIndexActor[K] extends Actor {
  import BuildIndexActors._
  private[this] var index: Map[Court, Set[K]] = Map.empty.withDefaultValue(Set.empty)
  private[this] var total: Int = -1
  private[this] var finished: Int = 0

  def receive = ({
    case Messages.AddCourtIndex(court, key: K) => {
      this.index = this.index + (court -> (this.index(court) + key))
      this.finished += 1
    }
    case Messages.SetTotalCount(cnt) => this.total = cnt
  }: Any --> Unit).andThen { _ =>
    if(total > 0 && total == finished) {
      context.parent ! Messages.RespondCourtIndex(index, finished)
      self ! PoisonPill
    }
  }
}

class BuildCaseNumberIndexActor[K] extends Actor {
  import BuildIndexActors._
  private[this] var index: Map[CaseNumber, Set[K]] = Map.empty.withDefaultValue(Set.empty)
  private[this] var total: Int = -1
  private[this] var finished: Int = 0

  def receive = ({
    case Messages.AddCaseNumberIndex(number, key: K) => {
      this.index = this.index + (number -> (this.index(number) + key))
      this.finished += 1
    }
    case Messages.SetTotalCount(cnt) => this.total = cnt
  }: Any --> Unit).andThen { _ =>
    if(total > 0 && total == finished) {
      context.parent ! Messages.RespondCaseNumberIndex(index, finished)
      self ! PoisonPill
    }
  }
}
