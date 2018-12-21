package me.nsmr
package lilib.search
package actors

import java.time.LocalDate
import scala.util.Try
import com.typesafe.scalalogging.Logger
import akka.actor.{ Props, ActorRef, ActorRefFactory, Actor }
import me.nsmr.lilib.core._

private[search] object MainActor {

  private[this] lazy val logger = Logger(this.getClass)

  def getActorRef[K](implicit sys: ActorRefFactory): ActorRef = sys.actorOf(this.props[K])

  def props[K]: Props = Props[MainActor[K]]

  object Messages {
    case class AddItem[K](key: K, value: Precedent)
    case class AddItemList[K](it: Iterator[(K, Precedent)])
    case object RequestResult
    case class RespondIndexes[K](
      dateIndex: Map[LocalDate, Set[K]],
      caseNumberIndex: Map[CaseNumber, Set[K]],
      courtIndex: Map[Court, Set[K]]
    )
    case class RespondDateIndex[K](map: Map[LocalDate, Set[K]])
    case class RespondCaseNumberIndex[K](map: Map[CaseNumber, Set[K]])
    case class RespondCourtIndex[K](map: Map[Court, Set[K]])
  }
}

private class MainActor[K] extends Actor {
  import MainActor.Messages._

  private[this] lazy val logger = Logger[MainActor[K]]

  private[this] var indexByDate: Map[LocalDate, Set[K]] = Map.empty
  private[this] var indexByCaseNumber: Map[CaseNumber, Set[K]] = Map.empty
  private[this] var indexByCourt: Map[Court, Set[K]] = Map.empty

  private[this] var countOfWaitingDate: Int = _
  private[this] var countOfWaitingCaseNumber: Int = _
  private[this] var countOfWaitingCourt: Int = _

  private[this] var requestSender: List[ActorRef] = Nil

  override def receive = ({
    case AddItem(key: K, value) => addItem(key, value)
    case AddItemList(list: Iterator[(K, Precedent)]) => addItemList(list)
    case BuildIndexActors.Messages.RespondDateIndex(response: Map[_, Set[K]], count) => addDateIndex(response, count)
    case BuildIndexActors.Messages.RespondCaseNumberIndex(response: Map[_, Set[K]], count) => addCaseNumberIndex(response, count)
    case BuildIndexActors.Messages.RespondCourtIndex(response: Map[_, Set[K]], count) => addCourtIndex(response, count)
    case RequestResult => requestResult(sender)
  }: PartialFunction[Any, Unit]).andThen { _ =>
    printinf(s"date: ${countOfWaitingDate} / case number: ${countOfWaitingCaseNumber} / Court: ${countOfWaitingCourt}")
    if(countOfWaitingDate == 0
      && countOfWaitingCaseNumber == 0
      && countOfWaitingCourt == 0
    ) {
      requestSender.foreach { sender =>
        sender ! RespondIndexes(indexByDate, indexByCaseNumber, indexByCourt)
      }
    }
  }

  def addItem(key: K, value: Precedent) = {
    try {
      this.indexByDate = this.indexByDate + (value.date -> (this.indexByDate.getOrElse(value.date, Set.empty) + key))
    } catch {
      case e: RuntimeException => {
        logger.warn(s"something went wrong while processing date of ${key}", e)
      }
    }

    try {
      this.indexByCaseNumber = this.indexByCaseNumber + (value.number -> (this.indexByCaseNumber.getOrElse(value.number, Set.empty) + key))
    } catch {
      case e: RuntimeException => {
        logger.warn(s"something went wrong while processing case number of ${key}", e)
      }
    }

    try {
      this.indexByCourt = this.indexByCourt + (value.court -> (this.indexByCourt.getOrElse(value.court, Set.empty) + key))
    } catch {
      case e: RuntimeException => {
        logger.warn(s"something went wrong while processing court of ${key}", e)
      }
    }
  }

  def addItemList(it: Iterator[(K, Precedent)]) = {
    logger.debug("addItemList is called")
    val dateActor = context.actorOf(BuildIndexActors.props.date[K])
    val caseNumberActor = context.actorOf(BuildIndexActors.props.caseNumber[K])
    val courtActor = context.actorOf(BuildIndexActors.props.court[K])

    var (countOfSentDates, countOfSentNumbers, countOfSentCourts) = (0, 0, 0)

    while(it.hasNext) { val (key, value) = it.next
      try {
        dateActor ! BuildIndexActors.Messages.AddDateIndex(value.date, key)
        countOfSentDates = countOfSentDates + 1
      } catch {
        case e: RuntimeException => {
          logger.warn(s"something went wrong while processing date of ${key}", e)
        }
      }

      try {
        caseNumberActor ! BuildIndexActors.Messages.AddCaseNumberIndex(value.number, key)
        countOfSentNumbers += 1
      } catch {
        case e: RuntimeException => {
          logger.warn(s"something went wrong while processing case number of ${key}", e)
        }
      }

      try {
        courtActor ! BuildIndexActors.Messages.AddCourtIndex(value.court, key)
        countOfSentCourts += 1
      } catch {
        case e: RuntimeException => {
          logger.warn(s"something went wrong while processing court of ${key}", e)
        }
      }
    }

    this.countOfWaitingDate = countOfSentDates
    this.countOfWaitingCourt = countOfSentCourts
    this.countOfWaitingCaseNumber = countOfSentNumbers
    dateActor ! BuildIndexActors.Messages.SetTotalCount(countOfSentDates)
    caseNumberActor ! BuildIndexActors.Messages.SetTotalCount(countOfSentNumbers)
    courtActor ! BuildIndexActors.Messages.SetTotalCount(countOfSentCourts)
    printinf(s"requests was successfully sent!: ${countOfWaitingDate} / ${countOfWaitingCourt} / ${countOfWaitingCaseNumber}")
  }

  def addDateIndex(response: Map[LocalDate, Set[K]], processedNumber: Int) = {
    if(this.countOfWaitingDate > 0) {
      this.indexByDate = this.indexByDate ++ response
      this.countOfWaitingDate -= processedNumber
    }
  }

  def addCaseNumberIndex(caseNumberIndex: Map[CaseNumber, Set[K]], processedNumber: Int) = {
    this.indexByCaseNumber = this.indexByCaseNumber ++ caseNumberIndex
    this.countOfWaitingCaseNumber -= processedNumber
  }

  def addCourtIndex(courtIndex: Map[Court, Set[K]], processedNumber: Int) = {
    this.indexByCourt = this.indexByCourt ++ courtIndex
    this.countOfWaitingCourt -= processedNumber
  }

  def requestResult(respondTo: ActorRef): Unit = {
    this.requestSender = respondTo :: this.requestSender
  }
}
