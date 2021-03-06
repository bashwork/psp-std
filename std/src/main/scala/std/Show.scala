package psp
package std

import all._, Show.by
import java.util.regex.Pattern
import scala.StringContext.processEscapes
import StdShow._

abstract class Printing {
  def pstream: PrintStream

  def dump(xs: Any*): Unit                        = pstream println (xs mkString ", ")
  def println[A](x: A)(implicit z: Show[A]): Unit = pstream println (z show x)
}
object out extends Printing {
  def pstream = scala.Console.out
}
object err extends Printing {
  def pstream = scala.Console.err
}

/** When a Show type class is more trouble than it's worth.
  *  Not overriding toString here to leave open the possibility of
  *  using a synthetic toString, e.g. of case classes.
  */
trait HasToS extends Any {
  def to_s: String
}
trait UseToS extends Any with HasToS {
  override def toString = to_s
}
trait UseDoc extends Any with UseToS {
  def doc: Doc
  def to_s: String = doc.pp
}
trait ShowSelf extends Any with UseToS

object StdShow extends StdShow

/** A not very impressive attempt to improve on string
  *  representations.
  */
sealed abstract class Doc {
  override def toString = abort("Doc.toString: " + this.pp)
}

object Doc {
  val SP = " ".lit

  final case object NoDoc                             extends Doc
  final case class Group(xs: View[Doc])               extends Doc
  final case class Cat(left: Doc, right: Doc)         extends Doc
  final case class Shown[A](value: A, shows: Show[A]) extends Doc
  final case class Literal(value: String)             extends Doc

  def empty: Doc                                    = NoDoc
  def apply[A](x: A)(implicit z: Show[A]): Shown[A] = Shown[A](x, z)
  def apply(s: String): Literal                     = Literal(s)
}

class FullRenderer(elemRange: SizeRange) extends Renderer {
  def minElements = elemRange.head
  def maxElements = elemRange.last

  private object UnderMax {
    def unapply(xs: View[Doc]) = xs splitAfter maxElements match {
      case SplitView(xs, EmptyView()) => Some(xs)
      case _                          => None
    }
  }
  def show(x: Doc): String = x match {
    case Doc.NoDoc               => ""
    case Doc.Cat(l, r)           => show(l) append show(r)
    case Doc.Group(UnderMax(xs)) => xs map (_.pp) joinWith ", " surround ("[ ", " ]")
    case Doc.Group(xs)           => xs take minElements map (_.pp) joinWith ", " surround ("[ ", ", ... ]")
    case Doc.Shown(value, z)     => z show value
    case Doc.Literal(s)          => s
  }
}

// MUST be called StringContext, hard-coded in Scala's parser
final case class StringContext(parts: String*) {

  /** TODO. See
    *  https://github.com/scala/scala/blob/2.12.x/src/compiler/scala/tools/reflect/FormatInterpolator.scala
    *  Can't see any way to call the standard (type-safe) f-interpolator.
    *
    *    private val FormatSpec = """%(?:(\d+)\$)?([-#+ 0,(\<]+)?(\d+)?(\.\d+)?([tT]?[%a-zA-Z])?""".r
    */
  /** Having args of type Doc* forces all the interpolated values
    * be of a type which is implicitly convertible to Doc.
    */
  def log(args: Doc*): Unit   = Pconfig.logger dump pp(args: _*)
  def pp(args: Doc*): String  = Pconfig.renderer show doc(args: _*)
  def fp(args: Doc*): String  = Pconfig.renderer show fdoc(args: _*)
  def sm(args: Doc*): String  = Pconfig.renderer show sdoc(args: _*)
  def any(args: Any*): String = pp(args.m asDocs Show.Inherited seq: _*)

  private def escapedParts: View[String]  = parts.toVec map processEscapes
  private def escaped: String             = escapedParts.join
  private def strippedParts: View[String] = escapedParts map (_ mapLines (_.stripMargin))

  /** There's one more escaped part than argument, so
    *  to collate them we tack an empty Doc onto the arg list.
    */
  def doc(args: Doc*): Doc  = escapedParts.asDocs.zip(args :+ Doc.empty).pairs flatMap (_.each) reducel (_ ~ _)
  def fdoc(args: Doc*): Doc = escaped.format(args.map(_.pp): _*)
  def sdoc(args: Doc*): Doc = new scala.StringContext(strippedParts.seq: _*).raw(args: _*).trim
}

/** An incomplete selection of show compositors.
  *  Not printing the way scala does.
  */
trait StdShow0 {
  implicit def showBoolean: Show[Boolean] = Show.Inherited
  implicit def showChar: Show[Char]       = Show.Inherited
  implicit def showByte: Show[Byte]       = Show.Inherited
  implicit def showShort: Show[Short]     = Show.Inherited
  implicit def showInt: Show[Int]         = Show.Inherited
  implicit def showLong: Show[Long]       = Show.Inherited
  implicit def showDouble: Show[Double]   = Show.Inherited
  implicit def showUnit: Show[Unit]       = Show.Inherited

  implicit def showView[A: Show]: Show[View[A]] = Show(xs => Doc.Group(xs.asDocs).pp)
  implicit def showIndex: Show[Index]           = by(_.indexValue)
}
trait StdShow2 extends StdShow0 {
  implicit def showPmap[K: Show, V: Show]: Show[Pmap[K, V]] = by(_.pairs mapLive (_.pp))
  implicit def showPset[A: Show]: Show[Pset[A]]             = by(_.basis.asShown.inBraces)

  implicit def showJavaMap[K: Show, V: Show]: Show[jMap[K, V]]   = Show(_.m.pairs map (_ mkDoc "=" pp) inBraces)
  implicit def showJavaIterable[A: Show]: Show[jIterable[A]]     = Show(_.m.asShown.inBrackets)
  implicit def showScalaIterable[A: Show]: Show[scIterable[A]]   = Show(xs => xs.m.asShown.inParens prepend xs.stringPrefix)
  implicit def showEach[A: Show]: Show[Each[A]]                  = by(_.m)
  implicit def showZipped[A1: Show, A2: Show]: Show[Zip[A1, A2]] = by(_.pairs)
  implicit def showArray[A: Show]: Show[Array[A]]                = by(_.m)
  implicit def showSplit[A: Show, R]: Show[SplitView[A, R]]      = Show(_ app (_.doc <+> "/" <+> _ pp))
}
trait StdShow3 extends StdShow2 {
  implicit def showString: Show[String]                     = Show.Inherited
  implicit def showThrowable: Show[Throwable]               = Show.Inherited
  implicit def showProvenance[A: Show]: Show[Provenance[A]] = by(_.get)

  implicit def showClass: Show[jClass]                  = Show(JvmName asScala _ short)
  implicit def showSelf: Show[ShowSelf]                 = Show(_.to_s)
  implicit def showNth: Show[Nth]                       = by(x => pp"#${x.nthValue}")
  implicit def showOption[A: Show]: Show[Option[A]]     = Show(_.fold("-")(_.pp))
  implicit def showPair[A: Show, B: Show]: Show[A -> B] = Show(_ mkDoc " -> " pp)
  implicit def showPattern: Show[Pattern]               = Show.Inherited

  implicit def showLiveView[A, B: Show]: Show[LiveView[A, B, _]] = by(_.lines.joinLines)

  implicit def showSize[A <: Size]: Show[A] = by(_.to_s)
}
trait StdShow extends StdShow3 {
  implicit def showInterval: Show[Interval] = by(_.to_s)

  /** Simpler versions of this method signature lead to implicit ambiguity.
    */
  implicit def showConsecutive[A, CC[X] <: Consecutive[X]]: Show[CC[_ <: A]] = by(_.in)
}
