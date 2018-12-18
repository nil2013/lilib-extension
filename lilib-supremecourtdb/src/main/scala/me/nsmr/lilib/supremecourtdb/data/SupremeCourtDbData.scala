package me.nsmr
package lilib.supremecourtdb

import com.itextpdf.kernel.pdf.{PdfReader, PdfDocument}

protected trait SupremeCourtDbData {

  /**
   * この判例を指示するIDを返却します。
   */
  def id: String
  /**
   * PdfDocument形式の判例データを返却します。
   */
  def getPdfDocument: PdfDocument
  /**
   * SupremeCourtDbPrecedent形式の判例についてのメタデータを返却します。
   */
  def getData: SupremeCourtDbPrecedent
  /**
   * 判例PDFのためのPdfDocumentを安全に使用します。
   */
  def reading[T](body: PdfDocument => T) = using(getPdfDocument){ body }
}

trait SupremeCourtDbDataProvider {
  /**
   * それぞれの判例データに逐次的にアクセスするためのイテレーターです。
   */
  def iterator: Iterator[SupremeCourtDbData]

  /**
   * IDを指定して判例データにアクセスします。
   */
  def getSupremeCourtData(id: String): Option[SupremeCourtDbData]

}
