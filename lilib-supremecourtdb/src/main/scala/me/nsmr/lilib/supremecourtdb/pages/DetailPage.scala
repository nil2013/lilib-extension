package me.nsmr
package lilib
package supremecourtdb
package pages

import java.net.URL

import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.{PdfDocument, PdfReader}
import org.jsoup.Jsoup
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._

object DetailPage {
  val SUPREME_COURT_DB_DETAIL_BASE_URL = "http://www.courts.go.jp/app/hanrei_jp/detail"

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

  def getUrlOf(pageType: Int, id: Int): String = s"${SUPREME_COURT_DB_DETAIL_BASE_URL}${pageType}?id=${id}"
}

class DetailPage(val id: Int, val pageType: Int = 2) {

  import DetailPage.PrecedentKey

  def url = DetailPage.getUrlOf(pageType, id)

  lazy val doc = Jsoup.connect(url).get

  lazy val content: Map[String, Element] = {
    val elements =
      doc.select("div.dlist").iterator.asScala.flatMap(_.children.asScala).filter { x =>
        x.tagName == "div" && x.className.isEmpty
      }
    elements.collect {
      // もうちょっと包括的に定義できないか
      case e if !(e.children.size < 2) => {
        val key = e.children.asScala.head.text.trim
        if (PrecedentKey.Content.name == key) {
          (key, e.selectFirst("a"))
        } else {
          (key, e.child(1))
        }
      }
    }.toMap
  }

  lazy val contentPdfUrl: Option[String] = {
    content.get("全文").flatMap { elem =>
      elem.select("a").asScala.find { as =>
        as.text == "全文"
      }.map {
        _.attr("abs:href")
      }
    }
  }

  /**
   *
   * @return
   */
  def getContentPdfProcessor[A](implicit con: PdfProcessorGenerator[A]): Option[A] = {
    contentPdfUrl.flatMap {
      con.generateProcessor
    }
  }

  def getFullText[A](implicit con: PdfProcessorGenerator[A]): Option[String] = this.contentPdfUrl.flatMap {
    con.convertPdfToText _
  }

  override def toString(): String = s"DetailPage(id = ${id}, pageType = ${pageType})"
}

trait PdfProcessorGenerator[A] {
  def generateProcessor(url: String): Option[A]

  def convertPdfToText(url: String): Option[String]
}

object ITextPdfProcessorGenerator extends PdfProcessorGenerator[PdfDocument] {

  override def generateProcessor(url: String): Option[PdfDocument] = {
    try {
      Option(new PdfDocument(new PdfReader((new URL(url)).openStream())))
    } catch {
      case e: Exception => None
    }
  }

  override def convertPdfToText(url: String): Option[String] = {
    generateProcessor(url).map {
      using(_) { doc =>
        (1 to doc.getNumberOfPages).iterator.map { page =>
          PdfTextExtractor.getTextFromPage(doc.getPage(page))
        }.mkString(System.lineSeparator())
      }
    }
  }

}