package me.nsmr
package lilib.analyzer

import java.io.{ BufferedReader, File, FileInputStream, InputStream }
import scala.util.matching.Regex
import me.nsmr.lilib.core.{ Court, SimpleCourt }

object CourtUtil {

  private[this] final lazy val defaultInstance: CourtUtil = new CourtUtil(
    this.getClass.getClassLoader.getResourceAsStream("courts.list"))

  def courts = defaultInstance.courts

  def places: Set[String] = defaultInstance.places

  def branches: Set[String] = defaultInstance.branches

}

class CourtUtil(val courts: Set[Court]) {
  import CourtUtil._

  def this(is: InputStream) = {
    this {
      FileUtil.reading(is) { it =>
        it.map(_.split(",")).flatMap {
          case Array(place, level, branch) =>
            try {
              Some(Court(place, level.toInt, branch))
            } catch {
              case e: NumberFormatException => e.printStackTrace; None
            }
          case Array(place, level) =>
            try {
              Some(Court(place, level.toInt, ""))
            } catch {
              case e: NumberFormatException => e.printStackTrace; None
            }
          case arr => println(arr.mkString("Array(", ", ", ")")); None
        }.toSet
      }
    }
  }

  def this(from: File) = this(new FileInputStream(from))

  def places: Set[String] = courts.iterator.map(_.place).toSet

  def branches: Set[String] = courts.iterator.map(_.branch).toSet

}
