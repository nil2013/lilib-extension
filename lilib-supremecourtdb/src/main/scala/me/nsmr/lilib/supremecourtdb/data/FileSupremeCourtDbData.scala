package me.nsmr
package lilib.supremecourtdb
package data

import java.io.{ File, IOException }
import java.time.LocalDate
import scala.xml.XML
import com.itextpdf.kernel.pdf.{PdfReader, PdfDocument}
import me.nsmr.utils.PdfTool
import me.nsmr.lilib.utils.{ Parsers, CaseNumberFormatException, CourtFormatException }
import me.nsmr.lilib.core.{ CaseNumber, Court, JudgeType, Precedent }

/**
 * ディレクトリにまとめられた判例データにアクセスするためのクラス
 */
class FileSupremeCourtDbData(protected val dir: File) extends SupremeCourtDbData[String] { data =>

  /**
   * 当該判例データに対応するPDFファイルを取得します。
   */
  private def pdfFile = new File(dir, "content.pdf")

  /**
   * 当該判例データについての情報を表現したXMLファイルを取得します。
   */
  private def infoFile = new File(dir, "info.xml")

  /**
   * 判例本文のテキストデータを取得します。
   * 存在しない場合や取得に失敗した場合には、Noneが返ります。
   */
  lazy val content: Option[String] = try {
    Option(PdfTool.extractTextFromPdfFile(data.pdfFile).mkString(System.lineSeparator))
  } catch {
    case e: Throwable => None
  }

  override def id: String = data.dir.getName

  override def getPdfDocument: PdfDocument = new PdfDocument(new PdfReader(this.pdfFile))

  override def getData: SupremeCourtDbPrecedent = new SupremeCourtDbPrecedent {
    val xml = XML.loadFile(infoFile)

    override def id: String = data.id

    override def number: CaseNumber = {
      val txt = (xml \ "caseNumber").text
      Parsers.caseNumber.parse(txt) match {
        case Some(cn) => cn
        case None => throw new CaseNumberFormatException(s"case number format is inappropriate: ${txt}")
      }
    }

    override def date: LocalDate = LocalDate.parse((xml \ "date").text, Precedent.dateFormatter)

    override def court: Court = {
      val txt = (xml \ "court").text
      Parsers.court.parse(txt) match {
        case Some(c) => c
        case None => throw new CourtFormatException(s"court format is inappropriate: ${txt}")
      }
    }

    override def judgeType: Option[JudgeType] = JudgeType.get((xml \ "type").text)

    override def content: Option[String] = data.content

    override def name: String = (xml \ "name").text
    override def result: String = (xml \ "result").text
    override def book: String = (xml \ "reporter").text

    override def previousCourt: Option[Court] = (xml \ "previous" \ "court").text.trim match {
      case "" => None
      case some => Parsers.court.parse(some) match {
        case Some(c) => Option(c)
        case None => throw new CourtFormatException(s"court format is inappropriate: ${some}")
      }
    }

    override def previousNumber: Option[CaseNumber] = ???
    override def previousDate: Option[LocalDate] = ???

    override def theme: String = (xml \ "theme").text.trim
    override def summary: String = (xml \ "summary").text.trim
    override def articles: Seq[String] = (xml \ "articles").text.split("[，,]").map(_.trim)

    override def pdfUrl: String = (xml \ "pdf").text.trim
  }
}

/**
 * ファイルシステム上に保存された判例データベースに対してアクセスするためのProvider
 */
class FileSupremeCourtDbDataProvider(val base: File =
  new File(System.getProperty("user.home"), "hanrei/data")) extends SupremeCourtDbDataProvider[String] {

  protected def isCorrectDirectory(dir: File): Boolean = {
    (dir.exists && dir.isDirectory && {
      val files = dir.list
      files.contains("content.pdf") && files.contains("info.xml")
    })
  }

  protected def isCorrectDirectory(name: String): Boolean = isCorrectDirectory(new File(base, name))

  override def iterator: Iterator[FileSupremeCourtDbData] = {
    base.list.iterator.map(new File(base, _)).collect {
      case dir if isCorrectDirectory(dir) => new FileSupremeCourtDbData(dir)
    }
  }

  def get(id: String): Option[FileSupremeCourtDbData] = {
    val dir = new File(base, id)
    if(isCorrectDirectory(dir)) {
      Option(new FileSupremeCourtDbData(dir))
    } else {
      None
    }
  }

  def ids: Iterator[String] = base.list.iterator.filter { fname => isCorrectDirectory(fname) }

  def size: Int = base.list.size
}