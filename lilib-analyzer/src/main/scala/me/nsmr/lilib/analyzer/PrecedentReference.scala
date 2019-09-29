package me.nsmr
package lilib.analyzer

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.JapaneseChronology
import me.nsmr.lilib.core.{CaseNumber, Court, JudgeType}
import com.typesafe.scalalogging.Logger

object PrecedentReference {
  lazy val dateFormatter = DateTimeFormatter.ofPattern("Gy年M月d日").withChronology(JapaneseChronology.INSTANCE)
}

case class PrecedentReference(caseNumber: Option[CaseNumber], court: Option[Court], date: Option[LocalDate], judgeType: Option[JudgeType]) {

  import PrecedentReference._

  override def toString = {
    val splitter = if (caseNumber.isDefined && date.isDefined) {
      "・"
    } else {
      ""
    }
    s"${court.map(_.shortName).mkString}${caseNumber.mkString}${splitter}${date.map(_.format(dateFormatter)).mkString}${court.map(_.branch).mkString}${judgeType.mkString}"
  }
}
