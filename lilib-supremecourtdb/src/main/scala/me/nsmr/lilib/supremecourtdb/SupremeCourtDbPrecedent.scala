package me.nsmr
package lilib
package supremecourtdb

import me.nsmr.lilib.core.{ Precedent, Court, CaseNumber }
import java.time.LocalDate

trait SupremeCourtDbPrecedent[K] extends Precedent {
  def name: String
  def result: String
  def book: String

  def previousCourt: Option[Court]
  def previousNumber: Option[CaseNumber]
  def previousDate: Option[LocalDate]

  def theme: String
  def summary: String
  def articles: Seq[String]

  def pdfUrl: String
  // def pdf:
  def id: K
}
