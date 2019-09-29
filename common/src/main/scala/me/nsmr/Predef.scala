package me.nsmr

import java.io.{
  BufferedWriter,
  File,
  FileInputStream,
  BufferedReader,
  InputStreamReader,
  InputStream,
  OutputStreamWriter,
  FileOutputStream
}
import scala.annotation.tailrec

object `package` {

  import scala.language.reflectiveCalls

  type -->[A, B] = PartialFunction[A, B]

  def itself[A]: A => A = (it => it)

  def using[A, R <: {def close()}](r: R)(body: R => A): A = try {
    body(r)
  } finally {
    r.close()
  }

  def elapsed[T](body: => T)(out: Long => Unit): T = {
    val begin = System.currentTimeMillis()
    val result = body
    out(System.currentTimeMillis() - begin)
    result
  }

  def benchmark[T](times: Int, mult: Int = 1)(body: => T): Unit = {
    val ts = (0 until times).map { _ =>
      val begin = System.currentTimeMillis()
      (0 until mult) foreach { _ => body }
      System.currentTimeMillis() - begin
    }
    if (times >= 3) {
      println(s"max: ${ts.max}ms, min: ${ts.min}ms")
    }
    println(s"average: ${ts.sum.toDouble / ts.size}ms")
  }

  implicit class IterableBufferedReader(br: java.io.BufferedReader) extends Iterable[String] {
    def iterator: Iterator[String] = Iterator.continually(br.readLine()).takeWhile(_ != null)
  }

  implicit class PowerfulString(str: String) {
    def powerTrim = str.dropWhile(c => c.isWhitespace).reverse.dropWhile(_.isWhitespace).reverse
  }

  // class ZipFunc1[-T, +U1, +U2](func1: T => U1, func2: T => U2) extends Function1[T, (U1, U2)] {
  //   def apply(arg: T): (U1, U2) = (func1(arg), func2(arg))
  // }
  implicit class Func1Extension[T1, U1](func1: Function1[T1, U1]) {
    def zip[T2 <: T1, U2](func2: Function1[T2, U2]): Function1[T2, (U1, U2)] = new Function1[T2, (U1, U2)] {
      def apply(arg: T2): (U1, U2) = (func1(arg), func2(arg))
    }
  }

  def printinf(str: String): Unit = print(s"\r\u001b[94m${str}\r")

  lazy val indicator = "|/-\\".toArray

  @tailrec final def await(condition: => Boolean, message: String = "", n: Int = 0): Unit = {
    printinf(s"${message} ${indicator(n % (indicator.size))}")
    Thread.sleep(50)
    if (!condition) await(condition, message, n + 1)
  }
}

object FileUtil {
  def reading[A](is: InputStreamReader)(body: Iterator[String] => A): A = using(new BufferedReader(is)) { br => body(br.iterator) }

  def reading[A](is: InputStream)(body: Iterator[String] => A): A = reading(new InputStreamReader(is)) {
    body
  }

  def reading[A](is: InputStream, encode: String)(body: Iterator[String] => A): A = reading(new InputStreamReader(is, encode)) {
    body
  }

  def reading[A](file: File)(body: Iterator[String] => A): A = reading(new FileInputStream(file)) {
    body
  }

  def reading[A](file: File, encode: String)(body: Iterator[String] => A): A = reading(new FileInputStream(file), encode) {
    body
  }

  def reading[A](path: String)(body: Iterator[String] => A): A = reading(new File(path))(body)

  def reading[A](path: String, encode: String)(body: Iterator[String] => A): A = reading(new File(path), encode)(body)

  def writing[A](os: OutputStreamWriter)(body: BufferedWriter => A): A = using(new BufferedWriter(os)) { writer => body(writer) }

  def writing[A](file: File, encode: String)(body: BufferedWriter => A): A = writing(new OutputStreamWriter(new FileOutputStream(file), encode)) {
    body
  }

  def writing[A](file: File)(body: BufferedWriter => A): A = writing(new OutputStreamWriter(new FileOutputStream(file))) {
    body
  }

  def writing[A](path: String, encode: String)(body: BufferedWriter => A): A = writing(new File(path), encode) {
    body
  }

  def writing[A](path: String)(body: BufferedWriter => A): A = writing(new File(path)) {
    body
  }

  def appending[A](file: File, encoding: String)(body: BufferedWriter => A): A = writing(new OutputStreamWriter(new FileOutputStream(file, true))) {
    body
  }

  def appending[A](path: String, encode: String)(body: BufferedWriter => A): A = writing(new OutputStreamWriter(new FileOutputStream(path, true))) {
    body
  }
}

object Colors {

  import java.io.PrintStream

  // implicit val stdout: PrintStream = System.out
  def coloring[A](color: Int, stream: PrintStream)(body: => A): A = coloring(new Color(color), stream)(body)

  def coloring[A](color: Int)(body: => A): A = coloring(new Color(color))(body)

  def coloring[A](color: Color, stream: PrintStream)(body: => A): A = try {
    color.start(stream); body
  } finally {
    color.reset(stream)
  }

  def coloring[A](color: Color)(body: => A): A = coloring(color, System.out)(body)

  sealed class Color(_ansi: Int) {
    def ansi: String = s"\u001b[${_ansi}m"

    def start(stream: PrintStream) = stream.print(ansi)

    def reset(stream: PrintStream) = Colors.Reset.start(stream)

    def using[A](stream: PrintStream)(body: PrintStream => A): A = coloring(this, stream)(body(stream))

    def using[A](body: => A): A = coloring(this)(body)
  }

  case object Reset extends Color(0)

  case object Red extends Color(31)

  case object Green extends Color(32)

  case object Yellow extends Color(33)

  case object Blue extends Color(34)

  case object Purple extends Color(35)

  case object LightBlue extends Color(36)

  case object White extends Color(37)

  case object RollingOver extends Color(7)

  case object LightGray extends Color(37)

  case object Gray extends Color(90)

}
