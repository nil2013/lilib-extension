package me.nsmr
package lilib
package supremecourtdb

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.HttpStatusException
import scala.collection.JavaConversions._
import scala.util.Try

object SupremeCourtDbLikeConnector {
  val SUPREME_COURT_DB_DETAIL_BASE_URL = "http://www.courts.go.jp/app/hanrei_jp/detail2"
  class PrecedentKey(val name: String) { override def toString(): String = s"Key(${name})" }
  object PrecedentKey {
    case object CaseNumber extends PrecedentKey("事件番号")
    case object CaseName extends PrecedentKey("事件名")
    case object CaseDate extends PrecedentKey("裁判年月日")
    case object Court extends PrecedentKey("法廷名")
    case object JudgeType extends PrecedentKey("裁判種別")
    case object Result extends PrecedentKey("結果")
    case object Book extends PrecedentKey("判例集等巻・号・頁")
    case object PreviousCourt extends PrecedentKey("原審裁判所名")
    case object PreviousCaseNumber extends PrecedentKey("原審事件番号")
    case object PreviousCaseDate extends PrecedentKey("原審裁判年月日")
    case object Theme extends PrecedentKey("判示事項")
    case object Summary extends PrecedentKey("裁判要旨")
    case object RelatedArticles extends PrecedentKey("参照法条")
    case object Content extends PrecedentKey("全文")

    def keys = Array(
      CaseNumber,
      CaseName,
      CaseDate,
      Court,
      JudgeType,
      Result,
      Book,
      PreviousCourt,
      PreviousCaseNumber,
      PreviousCaseDate,
      Theme,
      Summary,
      RelatedArticles,
      Content
    )

    def apply(key: String): PrecedentKey = keys.find(k => k.name == key).getOrElse(new PrecedentKey(key))
    def unapply(obj: PrecedentKey): Option[String] = Option(obj.name)
  }
}

class SupremeCourtDbLikeConnector(val base: String) {
  import SupremeCourtDbLikeConnector._

  def getUrlOf(id: Int): String = s"${base}?id=${id}"

  def specialRowParser: (PrecedentKey, Element) --> (PrecedentKey, Any) = {
    case (PrecedentKey.Content, elem) => (PrecedentKey.Content, elem.selectFirst("a").attr("abs:href"))
  }

  def rowParser: (PrecedentKey, Element) --> (PrecedentKey, Any) = specialRowParser.orElse( {
    case (key, elems) => (key, elems.children.tail.map(_.text).mkString.powerTrim)
  }: (PrecedentKey, Element) --> (PrecedentKey, Any))

  def getPrecedentMapById(id: Int): Try[Map[PrecedentKey, Any]] = Try {
    val doc = Jsoup.connect(getUrlOf(id)).get
    val element = doc.select("div.dlist").iterator.flatMap(_.children).filter(x => x.tagName == "div" && x.className.isEmpty)
    element.collect { case e if !(e.children.size < 2) =>
      val key = PrecedentKey(e.children.head.text.powerTrim)
      rowParser((key, e))
    }.toMap
  }
}

object SupremeCourtDbConnector extends SupremeCourtDbLikeConnector(SupremeCourtDbLikeConnector.SUPREME_COURT_DB_DETAIL_BASE_URL) {
  import SupremeCourtDbLikeConnector._

  override def specialRowParser: (PrecedentKey, Element) --> (PrecedentKey, String) = {
    case (PrecedentKey.Content, elem) => (PrecedentKey.Content, elem.selectFirst("a").attr("abs:href"))
  }

  override def rowParser: (PrecedentKey, Element) --> (PrecedentKey, String) = specialRowParser.orElse( {
    case (key, elems) => (key, elems.children.tail.map(_.text).mkString.powerTrim)
  }: (PrecedentKey, Element) --> (PrecedentKey, String))

  override def getPrecedentMapById(id: Int): Try[Map[PrecedentKey, String]] = Try {
    val doc = Jsoup.connect(getUrlOf(id)).get
    val element = doc.select("div.dlist").iterator.flatMap(_.children).filter(x => x.tagName == "div" && x.className.isEmpty)
    element.collect { case e if !(e.children.size < 2) =>
      val key = PrecedentKey(e.children.head.text.powerTrim)
      rowParser((key, e))
    }.toMap
  }

  def getPrecedentById(id: Int): SupremeCourtDbPrecedent[Int] = {
    val doc = Jsoup.connect(getUrlOf(id))
    ???
  }
}
