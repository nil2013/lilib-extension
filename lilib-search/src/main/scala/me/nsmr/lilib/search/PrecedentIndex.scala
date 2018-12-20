package me.nsmr
package lilib.search

import java.time.LocalDate
import me.nsmr.lilib.core.{ CaseNumber, Court, JudgeType }

object PrecedentIndex {
}

class PrecedentIndex[K] (
  val indexByDate: Map[LocalDate, Set[K]],
  val indexByNumber: Map[CaseNumber, Set[K]],
  val indexByCourt: Map[Court, Set[K]] ) {

  def indexes: Set[K] = (indexByDate :: indexByNumber :: indexByCourt :: Nil).map(_.values.reduce(_++_)).reduce(_++_)
  def dates: Set[LocalDate] = indexByDate.keySet
  def numbers: Set[CaseNumber] = indexByNumber.keySet
  def courts: Set[Court] = indexByCourt.keySet

  def getPrecedents(date: LocalDate): Set[K] = this.indexByDate(date)
  def getPrecedents(number: CaseNumber): Set[K] = this.indexByNumber(number)
  def getPrecedents(court: Court): Set[K] = {
    if(court.branch == null || court.branch.isEmpty) {
      val courts = this.courts.filter { c =>
        ( c.place == court.place && c.level == court.level )
      }
      courts.flatMap(this.indexByCourt(_))
    } else {
      this.indexByCourt(court)
    }
  }
}
