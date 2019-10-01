package me.nsmr
package lilib
package supremecourtdb
package pages

//import java.time.chrono.{JapaneseChronology, JapaneseDate}

import java.net.URLEncoder
import java.time.temporal.ChronoField
import java.util.Date
import org.jsoup.nodes.Element
import scala.jdk.CollectionConverters._

import me.nsmr.lilib.core.{CaseMark, CaseNumber, JapaneseYear, Court}
import org.jsoup.Jsoup

object ListPage {

  case class JapaneseDate(year: JapaneseYear, month: Int, date: Int)
  object Division {
    case object Minji extends Division("民事", 1)
    case object Keiji extends Division("刑事", 2)

    lazy val values = Array[Division](Minji, Keiji)
    def apply(n: Int): Division = values(n)
    def apply(name: String): Option[Division] = values.find(_.name == name)
    def apply(obj: Division): Int = values.indexOf(obj)
  }
  sealed class Division(val name: String, val idx: Int)

  object SearchFilter {
    def apply(dateSpecifier: DateSpecifier): SearchFilter = {
      this.apply(None, None, dateSpecifier)
    }

    def all = SearchFilter(
      SpanSpecifier(
        JapaneseDate(JapaneseYear(JapaneseYear.Era.Showa, 1), 1, 1),
        JapaneseDate(JapaneseYear(JapaneseYear.Era.Reiwa, 99), 12, 31)
      )
    )
  }

  /**
   * 裁判所裁判例検索ページの絞り込み条件を指定します。
   *
   * @param court         裁判所の指定。指定しない場合にはNoneを与えてください。
   * @param caseNumber    事件番号の指定。指定しない場合にはNoneを与えてください。
   * @param dateSpecifier 日付の指定。現在の実装では必ず指定する必要があります。
   * @param texts         検索キーワードの指定。指定しない場合にはNilを与えてください。
   */
  case class SearchFilter(
                           court: Option[Court],
                           caseNumber: Option[CaseNumber],
                           dateSpecifier: DateSpecifier,
                           division: Option[Division] = None,
                           texts: List[String] = Nil,
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

      val texts = {
        val ids = (1 to 9).iterator.map(n => s"text${n}").toList
        ids.zip(this.texts ::: ids.map(_ => ""))
      }
      ((courtToMap(court)
        ++ caseNumberToMap(caseNumber)
        ++ dateSpecifier.asMap
        ++ texts.toMap
        + ("division" -> division.map(_.idx).getOrElse(0)))
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
  def copy(pageType: Int = this.pageType, params: ListPage.SearchFilter = this.params, page: Option[Int] = this.page): ListPage =
    new ListPage(pageType, params, page)

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
      doc.select("div#next_buttom > div.s_title_r > a").asScala.find(_.text == "次へ→")
    )
  }

  def nextUrl: Option[String] = content._2.map(_.attr("abs:href"))

  def hasNext: Boolean = nextUrl.isDefined

  def nextPageNumber: Option[Int] = if (!hasNext) None else {
    val urlPat = s"${ListPage.BASE_URL}.*&page=([0-9]+).*".r
    this.nextUrl match {
      case Some(urlPat(p)) => Option(p.toInt)
      case _ => this.page match {
        case None => Some(2)
        case _ => this.page.map(_ + 1)
      }
    }
  }

  /** ちょっと妥協した。本当はnextUrlをパースしてListPageを再構築したい。 */
  def next: Option[ListPage] = {
    nextPageNumber.map { page =>
      new ListPage(pageType, params, Option(page))
    }
  }

  lazy val ids: List[Int] = content._1.map {
    case (elem, _, _) =>
      elem.attr("abs:href") match {
        case ListPage.DETAIL_URL_PATTERN(id) => id.toInt
        case url => throw new IllegalArgumentException(s"Illegal Pattern of URL: ${url}")
      }
  }

  def details: List[DetailPage] = ids.map {
    new DetailPage(_)
  }

  def size: Int = content._1.size

  lazy val total: Int = {
    val text = doc.selectFirst("h4.s_title_l")
    // println(text)
    ListPage.TOTAL_CASE_PATTERN.findFirstMatchIn(doc.selectFirst("h4.s_title_l").text) match {
      case None => -1
      case Some(m) => m.group(1).toInt
    }
  }

  override def toString(): String = {
    s"ListPage( pageType = ${pageType}, params = ${this.params}, page = ${this.page} )"
  }

}

