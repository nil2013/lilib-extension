package me.nsmr
package lilib.analyzer

import java.io.File
import java.time.LocalDate
import java.time.chrono.JapaneseDate
import com.typesafe.scalalogging.Logger
import me.nsmr.lilib.core._

class ReferenceAnalyzer {

  lazy val logger = Logger[ReferenceAnalyzer]

  private[this] final val charNormalizer: PartialFunction[Char, Char] = Map (
    '（' -> '(',
    '）' -> ')',
    '，' -> ',',
    '．' -> '.'
  ) ++ {
    "０１２３４５６７８９".zipWithIndex.map { case (k, v) => (k, v.toString.head) }.toMap
  } ++ {
    "〇一二三四五六七八九".zipWithIndex.map { case (k, v) => (k, v.toString.head) }.toMap
  } orElse (
    { case n if !n.isWhitespace => n }: PartialFunction[Char, Char]
  )

  object patterns {
    import scala.util.matching.Regex
    final lazy val courtPlaces = CourtUtil.courts.collect { case c if !c.place.isEmpty => c.place }.mkString("|")
    final lazy val court = s"(${courtPlaces})?(${(Court.LEVEL_SHORT.drop(1) ++ Court.LEVEL.drop(1)).mkString("|")})(?:裁|裁判所)|(?:最高裁|最高裁判所)"
    final lazy val branch = CourtUtil.courts.collect { case court if !court.branch.isEmpty => court.branch }.mkString("|")
    final val caseNumber = s"""(平成|昭和)([0-9]+|元)年\\(([^\\(\\).]+?)\\)第([0-9]+)号"""
    final val caseDate = s"""(平成|昭和|同)([0-9]+|元)?年([0-9]+)月([0-9]+)日"""
    final val judgeTypes = s"判決|決定"
    final lazy val fullRegex = new Regex(s"""(${court})(${caseNumber})?・?(${caseDate})(${branch})?(${judgeTypes})""".map(charNormalizer),
      "court", "court.place", "court.level",
      "case", "case.era", "case.year", "case.mark", "case.index",
      "date", "date.era", "date.year", "date.month", "date.date",
      "court.branch",
      "type"
    )
  }

  def analyze(input: String): Array[PrecedentReference] = normalize(input).flatMap(analyzeMain)

  def normalize(input: String): Array[String] = {
    input.lines.map(_.collect ( charNormalizer )).filterNot(
      l => l.matches(""".{0,2}[0-9]+.{0,2}""")).mkString("").split("[。、,¥¥.]")
  }

  /**
   * 以下のような形式のテキストを判例本文情報から抽出し、返却する。
   * [裁判所名(長/短)][元号][Y1]年([分類番号])第[事件番号]号[元号or"同"][Y2]年[M]月[D]日[法廷名]["判決" or "決定"]・[参照文献]
   * ex) 最高裁昭和５３年（オ）第１２４０号同６０年１１月２１日第一小法廷判決・民集３９巻７号１５１２頁参照
   */
  private[this] def analyzeMain(normalized: String): Array[PrecedentReference] = {
    //
    // いい感じに正規表現にして、findAllInを利用して列挙するのが正解か。
    val parsed = patterns.fullRegex.findAllMatchIn(normalized).map { info =>
      val court = try {
        Option(info.group("court")).flatMap { court =>
          val branch = Option(info.group("court.branch")).mkString
          val (level, place) = info.group("court.level") match {
            case null => (0, "")
            case l => (Court.levelOf(l), info.group("court.place"))
          }
          if(
            ((place == null || place.isEmpty) && level != 0)
            || (level != 0 && !branch.isEmpty && !branch.endsWith("支部"))
          ) {
            printinf("")
            logger.warn(s"strange court found: ${court} in ${info.source}")
            None
          } else {
            CourtUtil.courts.find ( c => c.level == level && c.place == place && c.branch == branch) match {
              case some: Some[Court] => some
              case None => Option(SimpleCourt(place, level, branch))
            }
          }
        }
      } catch {
        case e: Throwable =>
          logger.error(s"Something went wrong when parsing '${info.group("court")}' as court", e)
          None
      }
      val caseNumber = try {
        Option(info.group("case")).flatMap { _ =>
          CaseYear.Era(info.group("case.era")).map { era =>
            val year = info.group("case.year") match {
              case "元" => 1
              case n => n.toInt
            }
            SimpleCaseNumber(CaseYear(era, year), info.group("case.mark"), info.group("case.index").toInt)
          }
        }
      } catch {
        case e: Throwable =>
          logger.error(s"Something went wrong when parsing '${info.group("case")}' as case", e)
          None
      }
      val date = try {
        Option(info.group("date")).flatMap { _ =>
          val year = {
            val yearNum = info.group("date.year") match {
              case null => None
              case "元" => Some(1)
              case n => Option(n.toInt)
            }
            if(info.group("date.era") == "同") {
              val y = caseNumber.map(_.year)
              yearNum match {
                case None => y
                case Some(year) => y.map(_.copy(year = year))
              }
            } else {
              yearNum.flatMap { y => CaseYear.Era(info.group("date.era")).map { CaseYear(_, y) }}
            }
          }
          year.map { year =>
            LocalDate.from(JapaneseDate.of(year.era.javaEra, year.year, info.group("date.month").toInt, info.group("date.date").toInt))
          }
        }
      } catch {
        case e: IllegalStateException =>
          logger.error(s"Something went wrong when parsing '${info.group("date")}' as date")
          logger.error(s"match error: ${info.groupNames.map(k => (k, info.group(k)))}", e)
          None
        case e: Throwable =>
          logger.warn(s"Something went wrong when parsing '${info.group("date")}' as date", e)
          logger.warn(info.groupNames.map(n => (n, info.group(n))).mkString(", "))
          None
      }
      val judgeType = try {
        Option(info.group("type")).flatMap { JudgeType.get(_) }
      } catch {
        case e: Throwable => {
          logger.warn(s"Something went wrong when parsing '${info.group("type")}' as judgeType", e.toString)
          None
        }
      }
      PrecedentReference(caseNumber, court, date, judgeType)
    }
    parsed.toArray
  }
}
