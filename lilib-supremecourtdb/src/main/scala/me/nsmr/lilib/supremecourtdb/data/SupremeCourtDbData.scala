package me.nsmr
package lilib.supremecourtdb
package data

import com.itextpdf.kernel.pdf.{PdfReader, PdfDocument}

/**
 * 最高裁判例データベースに対応した形式の判例データを取り扱う機能を持つことを意味するTraitです。
 */
trait SupremeCourtDbData[K] {

  /**
   * この判例を指示するIDを返却します。
   */
  def id: K

  /**
   * PdfDocument形式の判例データを返却します。
   */
  def getPdfDocument: PdfDocument

  /**
   * SupremeCourtDbPrecedent形式に変換します。
   */
  def asPrecedent: SupremeCourtDbPrecedent[K]

  /**
   * 判例PDFのためのPdfDocumentを安全に使用します。
   */
  def reading[T](body: PdfDocument => T) = using(getPdfDocument) {
    body
  }
}

/**
 * 最高裁判例データベースに対応した形式の判例データの集合に対してアクセスする機能を持つことを意味するTraitです。
 */
trait SupremeCourtDbDataProvider[K] extends Iterable[SupremeCourtDbData[K]] {
  /**
   * それぞれの判例データに逐次的にアクセスするためのイテレーターです。
   */
  def iterator: Iterator[SupremeCourtDbData[K]]

  /**
   * IDを指定して判例データにアクセスします。
   */
  def get(id: K): Option[SupremeCourtDbData[K]]

}
