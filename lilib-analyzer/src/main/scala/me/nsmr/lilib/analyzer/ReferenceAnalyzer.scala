package me.nsmr
package lilib.analyzer

import java.io.File
import java.time.LocalDate
import java.time.chrono.JapaneseDate
import com.typesafe.scalalogging.Logger
import me.nsmr.lilib.core._

class ReferenceAnalyzer {

  lazy val logger = Logger[ReferenceAnalyzer]

  private[this] var last: (Option[JapaneseYear.Era], Option[Int], Option[Court]) = (None, None, None)

  private[this] final val charNormalizer: PartialFunction[Char, Char] = Map(
    '（' -> '(',
    '）' -> ')',
    '，' -> ',',
    '．' -> '.'
  ) ++ {
    "０１２３４５６７８９".zipWithIndex.map { case (k, v) => (k, v.toString.head) }.toMap
  } ++ {
    "〇一二三四五六七八九".zipWithIndex.map { case (k, v) => (k, v.toString.head) }.toMap
  } orElse (
    {
      case n if !n.isWhitespace => n
    }: PartialFunction[Char, Char]
    )

  object patterns {

    import scala.util.matching.Regex

    final lazy val courtPlaces = CourtUtil.courts.collect { case c if !c.place.isEmpty => c.place }.mkString("|")
    final lazy val court = s"(${courtPlaces}|同)?(${(Court.LEVEL_SHORT.drop(1) ++ Court.LEVEL.drop(1)).mkString("|")}|同)(?:裁|裁判所)|(?:最高裁|最高裁判所)"
    final lazy val branch = CourtUtil.courts.collect { case court if !court.branch.isEmpty => court.branch.map(charNormalizer) }.mkString("|")
    final val caseNumber = s"""(平成|昭和)([0-9]+|元)年?(([^\\d]?)\\(([^\\(\\)]+)\\)([^\\d第]?))第?([0-9]+)号"""
    final val caseDate = s"""(平成|昭和|同)([0-9]+|元)?年([0-9]+)月([0-9]+)日"""
    final val judgeTypes = s"判決|決定"
    final lazy val fullRegex = new Regex(s"""(${court})?(${caseNumber})?・?(${caseDate})(${branch})?(${judgeTypes})""",
      "court", "court.place", "court.level",
      "case", "case.era", "case.year", "case.mark", "case.mark.pre", "case.mark.mark", "case.mark.suf", "case.index",
      "date", "date.era", "date.year", "date.month", "date.date",
      "court.branch",
      "type"
    )
  }

  def reset: Unit = {
    this.last = (None, None, None)
  }

  def lastEra: Option[JapaneseYear.Era] = this.last._1

  def lastYear: Option[JapaneseYear] = {
    this.last match {
      case (Some(era), Some(year), _) => Option(JapaneseYear(era, year))
      case _ => None
    }
  }

  def lastCourt: Option[Court] = this.last._3

  protected def updateLastEra(era: JapaneseYear.Era): Unit = {
    this.last = this.last.copy(_1 = Option(era))
  }

  protected def updateLastYear(year: JapaneseYear): Unit = {
    this.last = this.last.copy(_1 = Option(year.era), _2 = Option(year.year))
  }

  protected def updateLastCourt(court: Court): Unit = {
    this.last = this.last.copy(_3 = Option(court))
  }

  def analyze(input: String): Array[PrecedentReference] = {
    reset
    normalize(input).flatMap(analyzeMain)
  }

  def normalize(input: String): Array[String] = {
    input.lines.map(_.collect(charNormalizer)).filterNot(
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
          // val (level, place) = info.group("court.level") match {
          //   case null => (0, "")
          //   case l => (Court.levelOf(l), info.group("court.place"))
          // }
          val (level, place) = {
            (info.group("court.level"), info.group("court.place"), branch) match {
              case ("同", _, _) => this.lastCourt.map { c => (c.level, c.place) }.get
              case (_, "同", _) => this.lastCourt.map { c => (c.level, c.place) }.get
              case (null, _, _) => (0, "")
              case (l, null, b) => {
                if (b.endsWith("法廷") && Court.levelOf(l) == 1) {
                  (0, "")
                } else {
                  (Court.levelOf(l), "")
                }
              }
              case (l, p, _) => (Court.levelOf(l), p)
            }
          }
          if (
            ((place == null || place.isEmpty) && level != 0)
              || (level != 0 && !branch.isEmpty && !branch.endsWith("支部"))
          ) {
            printinf("")
            logger.warn(s"strange court found: ${court} in ${info.source} (interpreted as: [level=${level}, place='${place}', branch='${branch}'], while the last court found is '${this.lastCourt}')")
            None
          } else {
            val obj = CourtUtil.courts.find(c => c.level == level && c.place == place && c.branch == branch) match {
              case some: Some[Court] => some
              case None => Option(SimpleCourt(place, level, branch))
            }
            obj.foreach(updateLastCourt)
            obj
          }
        }
      } catch {
        case e: Throwable =>
          logger.error(s"Something went wrong when parsing '${info.group("court")}' as court", e)
          None
      }
      val caseNumber = try {
        Option(info.group("case")).flatMap { _ =>
          JapaneseYear.Era(info.group("case.era")).map { era =>
            val year = info.group("case.year") match {
              case "元" => 1
              case n => n.toInt
            }
            val mark = {
              val mark = CaseMark(info.group("case.mark.mark"))
              (info.group("case.mark.pre"), info.group("case.mark.suf")) match {
                case (p, _) if !p.isEmpty => mark.withPrefix(p)
                case (_, s) if !s.isEmpty => mark.withSuffix(s)
                case (p, s) if p.isEmpty && s.isEmpty => mark
                case _ => throw new UnsupportedOperationException(s"mark '${info.group("case.mark")}' has both prefix and suffix")
              }
            }
            val jpYear = JapaneseYear(era, year)
            updateLastYear(jpYear)
            SimpleCaseNumber(jpYear, mark, info.group("case.index").toInt)
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
            if (info.group("date.era") == "同") {
              val y = this.lastYear
              yearNum match {
                case None => y
                case Some(year) => {
                  val obj = y.map(_.copy(year = year))
                  obj.foreach(updateLastYear)
                  obj
                }
              }
            } else {
              yearNum.flatMap { y => JapaneseYear.Era(info.group("date.era")).map {
                JapaneseYear(_, y)
              }
              }
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
        Option(info.group("type")).flatMap {
          JudgeType.get(_)
        }
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
