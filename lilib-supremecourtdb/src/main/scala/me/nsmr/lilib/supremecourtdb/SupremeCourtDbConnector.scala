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
}

class SupremeCourtDbLikeConnector(val base: String) {
  import SupremeCourtDbLikeConnector._
  type Parser = (String, Element) --> (String, Any)

  def getUrlOf(id: Int): String = s"${base}?id=${id}"

  def specialRowParser: Parser = Map.empty

  def rowParser: Parser = specialRowParser.orElse( {
    case (key, elems) => (key, elems.children.tail.map(_.text).mkString.powerTrim)
  }: Parser)

  def getPrecedentMapById(id: Int): Try[Map[String, Any]] = Try {
    val doc = Jsoup.connect(getUrlOf(id)).get
    val element = doc.select("div.dlist").iterator.flatMap(_.children).filter(x => x.tagName == "div" && x.className.isEmpty)
    element.collect { case e if !(e.children.size < 2) =>
      val key = e.children.head.text.powerTrim
      rowParser((key, e))
    }.toMap
  }
}

object SupremeCourtDbConnector extends SupremeCourtDbLikeConnector(
  SupremeCourtDbLikeConnector.SUPREME_COURT_DB_DETAIL_BASE_URL
) {
  import SupremeCourtDbLikeConnector._

  object Keys {
    val number = "事件番号"
    val name = "事件名"
    val date = "裁判年月日"
    val court = "法廷名"
    val judgeType = "裁判種別"
    val result = "結果"
    val report = "判例集等巻・号・頁"
    val originalCourt = "原審裁判所名"
    val originalNumber = "原審事件番号"
    val originalDate = "原審裁判年月日"
    val theme = "判示事項"
    val summary = "裁判要旨"
    val relatedArticles = "参照法条"
    val content = "全文"

    def all = Set(
      number,
      name,
      date,
      court,
      judgeType,
      result,
      report,
      originalCourt,
      originalNumber,
      originalDate,
      theme,
      summary,
      relatedArticles,
      content,
    )
  }

  override def specialRowParser: (String, Element) --> (String, String) = {
    case (Keys.content, elem) => (Keys.content, elem.selectFirst("a").attr("abs:href"))
  }

  override def rowParser: (String, Element) --> (String, String) = specialRowParser.orElse( {
    // 本当はここに、意図されていないキーが含まれていた場合の処理も書いておきたい
    case (key, elems) => (key, elems.children.tail.map(_.text).mkString.powerTrim)
  }: (String, Element) --> (String, String))

  override def getPrecedentMapById(id: Int): Try[Map[String, String]] = Try {
    val doc = Jsoup.connect(getUrlOf(id)).get
    val element = doc.select("div.dlist").iterator.flatMap(_.children).filter(x => x.tagName == "div" && x.className.isEmpty)
    element.collect { case e if !(e.children.size < 2) =>
      val key = e.children.head.text.powerTrim
      rowParser((key, e))
    }.toMap
  }

  def getPrecedentById(id: Int): SupremeCourtDbPrecedent[Int] = {
    val doc = Jsoup.connect(getUrlOf(id))
    ???
  }
}
