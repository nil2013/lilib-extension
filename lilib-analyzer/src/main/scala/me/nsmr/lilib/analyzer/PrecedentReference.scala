package me.nsmr
package lilib.analyzer

import java.time.LocalDate
import me.nsmr.lilib.core.{ CaseNumber, Court, JudgeType }
import com.typesafe.scalalogging.Logger

case class PrecedentReference(caseNumber: Option[CaseNumber], court: Option[Court], date: Option[LocalDate], judgeType: Option[JudgeType])
