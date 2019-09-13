package me.nsmr
package utils

import java.io.File
import java.net.{URL}
import com.itextpdf.kernel.pdf.{PdfReader, PdfDocument}
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser
import com.itextpdf.kernel.pdf.canvas.parser.listener.{
  ITextExtractionStrategy, SimpleTextExtractionStrategy}
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import scala.collection.JavaConverters._

object PdfTool {
  def extractTextFromPdfFile(file: File): Array[String] =
    using(new PdfDocument(new PdfReader(file))) {
      doc => extractTextFromPdfDoc(doc) }

  def extractTextFromPdfDoc(doc: PdfDocument): Array[String] = {
    val parser = new PdfDocumentContentParser(doc)
    def str = new SimpleTextExtractionStrategy
    (1 to doc.getNumberOfPages).map { p =>
      parser.processContent(p, str).getResultantText
    }.toArray
  }
}
