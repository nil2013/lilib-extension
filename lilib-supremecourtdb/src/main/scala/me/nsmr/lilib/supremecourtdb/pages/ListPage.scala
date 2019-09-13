package me.nsmr
package lilib
package supremecourtdb
package pages

//import java.time.chrono.{JapaneseChronology, JapaneseDate}

import java.net.URLEncoder
import java.time.temporal.ChronoField
import java.util.Date
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._

import me.nsmr.lilib.core.{CaseMark, CaseNumber, CaseYear, Court}
import org.jsoup.Jsoup

object ListPage {

  case class JapaneseDate(year: CaseYear, month: Int, date: Int)

  object SearchFilter {
    def all = SearchFilter(
      None,
      None,
      SpanSpecifier(
        JapaneseDate(CaseYear(CaseYear.Era.Showa, 1), 1, 1),
        JapaneseDate(CaseYear(CaseYear.Era.Reiwa, 99), 12, 31)
      ),
      Nil
    )
  }

  case class SearchFilter(
                           court: Option[Court],
                           caseNumber: Option[CaseNumber],
                           dateSpecifier: DateSpecifier,
                           texts: List[String],
                         ) {
    private def courtToMap(court: Option[Court]): Map[String, String] = Map(
      "courtName" -> court.map(_.place).getOrElse(""),
      "courtType" -> court.map(c => Court.LEVEL(c.level)).getOrElse(""),
      "branchName" -> court.map(_.branch).getOrElse("")
    )

    private def caseNumberToMap(caseNumber: Option[CaseNumber]): Map[String, String] = Map(
      "jikenGengo" -> caseNumber.map(_.year.era.name).getOrElse(""),
      "jikenYear" -> caseNumber.map(_.year.year.toString).getOrElse(""),
      "jikenCode" -> caseNumber.map(_.mark.toString).getOrElse(""),
      "jikenNumber" -> caseNumber.map(_.index.toString).getOrElse("")
    )

    def asQuery: String = {
      import URLEncoder.encode

      ((courtToMap(court)
        ++ caseNumberToMap(caseNumber)
        ++ dateSpecifier.asMap
        ++ (
        texts.zipWithIndex.map {
          case (text, idx) =>
            s"text${idx + 1}" -> text
        }
        ).toMap)
        .map {
          case (key, value) =>
            (encode(s"filter[$key]", "UTF-8"), encode(s"$value", "UTF-8"))
        }
        + ("action_search" -> "検索"))
        .map { case (k, v) => s"$k=$v" }
        .mkString("&")
    }
  }

  sealed trait DateSpecifier {
    def asMap: Map[String, String]
  }

  case object NoSpecify extends DateSpecifier {
    override def asMap: Map[String, String] = Map(
      "judgeDateMode" -> "",
      "judgeGengoFrom" -> "",
      "judgeYearFrom" -> "",
      "judgeMonthFrom" -> "",
      "judgeDayFrom" -> "",
      "judgeGengoFrom" -> "",
      "judgeYearTo" -> "",
      "judgeMonthTo" -> "",
      "judgeDayTo" -> "",
    )
  }

  case class SingleDateSpecifier(date: JapaneseDate) extends DateSpecifier {
    override def asMap: Map[String, String] = Map(
      "judgeDateMode" -> "1",
      "judgeGengoFrom" -> date.year.era.name,
      "judgeYearFrom" -> date.year.year.toString,
      "judgeMonthFrom" -> date.month.toString,
      "judgeDayFrom" -> date.date.toString,
      "judgeGengoTo" -> "",
      "judgeYearTo" -> "",
      "judgeMonthTo" -> "",
      "judgeDayTo" -> ""
    )
  }

  case class SpanSpecifier(from: JapaneseDate, to: JapaneseDate) extends DateSpecifier {
    override def asMap: Map[String, String] = Map(
      "judgeDateMode" -> "2",
      "judgeGengoFrom" -> from.year.era.name,
      "judgeYearFrom" -> from.year.year.toString,
      "judgeMonthFrom" -> from.month.toString,
      "judgeDayFrom" -> from.date.toString,
      "judgeGengoTo" -> to.year.era.name,
      "judgeYearTo" -> to.year.year.toString,
      "judgeMonthTo" -> to.month.toString,
      "judgeDayTo" -> to.date.toString,
    )
  }

  val BASE_URL = "http://www.courts.go.jp/app/hanrei_jp/list"

  def getUrlOf(pageType: Int, params: SearchFilter): String = {
    s"${BASE_URL}${pageType}?${params.asQuery}"
  }

  def getUrlOf(pageType: Int, params: SearchFilter, page: Int): String = {
    s"${BASE_URL}${pageType}?${params.asQuery}&page=${page}"
  }

  private lazy val DETAIL_URL_PATTERN = s"${DetailPage.SUPREME_COURT_DB_DETAIL_BASE_URL}[0-9]+\\?id=([0-9]+)".r

  private lazy val TOTAL_CASE_PATTERN = s"([0-9]+)件中[0-9]+～[0-9]+件を表示".r
}

class ListPage(
                val pageType: Int = 1,
                val params: ListPage.SearchFilter,
                val page: Option[Int] = None
              ) {
  def url: String = page match {
    case None => ListPage.getUrlOf(pageType, params)
    case Some(page) => ListPage.getUrlOf(pageType, params, page)
  }

  lazy val doc = Jsoup.connect(url).get

  lazy val content: (List[(Element, Element, Element)], Option[Element]) = {
    (
      doc.select("div#list tr").asScala.map {
        elem => {
          (
            elem.selectFirst("td > a"),
            elem.select("td").get(1),
            elem.select("td > a").get(1))
        }
      }.toList,
      Option(doc.selectFirst("div#next_buttom > div.s_title_r > a"))
    )
  }

  def nextUrl: Option[String] = content._2.map(_.attr("abs:href"))

  def hasNext: Boolean = nextUrl.isDefined

  /** ちょっと妥協した。本当はnextUrlをパースしてListPageを再構築したい。 */
  def next: Option[ListPage] = if (!hasNext) None else {
    Option(new ListPage(
      pageType, params,
      page match {
        case None => Some(1)
        case Some(page) => Some(page + 1)
      }
    ))
  }

  def details: List[DetailPage] = content._1.map {
    case (elem, _, _) =>
      elem.attr("abs:href") match {
        case ListPage.DETAIL_URL_PATTERN(id) => new DetailPage(id.toInt)
        case url => throw new IllegalArgumentException(s"Illegal Pattern of URL: ${url}")
      }
  }

  def size: Int = content._1.size

  lazy val total: Int = {
    val text = doc.selectFirst("h4.s_title_l")
    println(text)
    ListPage.TOTAL_CASE_PATTERN.findFirstMatchIn(doc.selectFirst("h4.s_title_l").text) match {
      case None => -1
      case Some(m) => m.group(1).toInt
    }
  }

}

