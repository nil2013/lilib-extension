package me.nsmr
package lilib
package supremecourtdb

import org.jsoup.Jsoup
import org.jsoup.HttpStatusException
import scala.collection.JavaConverters._

object SupremeCourtDbConnector {
  val SUPREME_COURT_DB_DETAIL_BASE_URL = "http://www.courts.go.jp/app/hanrei_jp/detail2"

  sealed class PrecedentKey(val name: String) {
    override def toString(): String = s"Key(${name})"
  }

  object PrecedentKey {

    object CaseNumber extends PrecedentKey("事件番号")

    object CaseName extends PrecedentKey("事件名")

    object CaseDate extends PrecedentKey("裁判年月日")

    object Court extends PrecedentKey("法廷名")

    object JudgeType extends PrecedentKey("裁判種別")

    object Result extends PrecedentKey("結果")

    object Book extends PrecedentKey("判例集等巻・号・頁")

    object PreviousCourt extends PrecedentKey("原審裁判所名")

    object PreviousCaseNumber extends PrecedentKey("原審事件番号")

    object PreviousCaseDate extends PrecedentKey("原審裁判年月日")

    object Theme extends PrecedentKey("判示事項")

    object Summary extends PrecedentKey("裁判要旨")

    object RelatedArticles extends PrecedentKey("参照法条")

    object Content extends PrecedentKey("全文")

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

class SupremeCourtDbConnector() {

  import SupremeCourtDbConnector._

  def getPrecedentById(id: Int): SupremeCourtDbPrecedent[Int] = {
    val doc = Jsoup.connect(getUrlOf(id))
    ???
  }

  def getPrecedentMapById(id: Int): Option[Map[PrecedentKey, String]] = {
    try {
      val doc = Jsoup.connect(getUrlOf(id)).get
      val elements = doc.select("div.dlist").iterator.asScala.flatMap(
        _.children.asScala).filter(x => x.tagName == "div" && x.className.isEmpty)
      Some(
        elements.collect {
          case e if !(e.children.size < 2) => PrecedentKey(e.children.first.text.trim) match {
            case PrecedentKey.Content => (PrecedentKey.Content, e.selectFirst("a").attr("abs:href"))
            case key => (key, e.child(2).text.trim)
          }
        }.toMap
      )
    } catch {
      case e: HttpStatusException => None
    }
  }

  def getUrlOf(id: Int): String = s"${SUPREME_COURT_DB_DETAIL_BASE_URL}?id=${id}"
}
