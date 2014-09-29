package psp
package std

/** The big bundle of everything which goes into the standard package object.
 */
abstract class PackageLevel extends PackageImplicits with api.PackageLevel with api.JavaIO with PackageAliases with PackageMethods {
  val NoIndex      = Index.undefined
  val NoNth        = Nth.undefined
  val NumericRange = scala.collection.immutable.NumericRange
  val ScalaNil     = scala.collection.immutable.Nil
}

trait PackageAliases {
  self: PackageLevel =>

  type ForeachableType[A0, Repr, CC0[X]] = Foreachable[Repr] {
    type A = A0
    type CC[B] = CC0[B]
  }
  type DirectAccessType[A0, Repr, CC0[X]] = DirectAccess[Repr] {
    type A = A0
    type CC[B] = CC0[B]
  }

  type Show[-A]         = api.Show[A]
  type Eq[-A]           = api.Eq[A]
  type HashEq[-A]       = api.HashEq[A]
  type Order[-A]        = api.Order[A]
  type PartialOrder[-A] = api.PartialOrder[A]
  type Read[A]          = api.Read[A]
  type ShowDirect       = api.ShowDirect

  type Invariant[A] = api.Invariant[A]
  type Foreach[+A]  = api.Foreach[A]
  type Indexed[+A]  = api.Indexed[A]
  type Direct[+A]   = api.Direct[A]
  type Linear[+A]   = api.Linear[A]
  type SizeInfo     = api.SizeInfo
  type Precise      = api.Precise
  type Bounded      = api.Bounded
  type Atomic       = api.Atomic

  val Infinite = api.Infinite
  val Precise  = api.Precise
  val Bounded  = api.Bounded
  val Eq       = api.Eq
  val Show     = api.Show
}

trait PackageMethods {
  self: PackageLevel =>

  def show[A: Show] : Show[A]        = ?
  // def readInto[A] : Read.ReadInto[A] = Read.into[A]

  def precise(n: Int): Precise = Precise(Size(n))
  def bounded(lo: Size, hi: SizeInfo): SizeInfo = hi match {
    case hi: Atomic     => bounded(lo, hi)
    case Bounded(_, hi) => bounded(lo, hi)
  }
  def bounded(lo: SizeInfo, hi: SizeInfo): SizeInfo = lo match {
    case Precise(lo)    => bounded(lo, hi)
    case Bounded(lo, _) => bounded(lo, hi)
    case Infinite       => Infinite
  }
  def bounded(lo: Size, hi: Atomic): SizeInfo = hi match {
    case Precise(n) if n < lo  => SizeInfo.Empty
    case Precise(n) if n == lo => hi
    case _                     => Bounded(lo, hi)
  }

  def contextLoader(): ClassLoader                       = noNull(Thread.currentThread.getContextClassLoader, nullLoader)
  def decodeName(s: String): String                      = scala.reflect.NameTransformer decode s
  def each[A](xs: GTOnce[A]): Foreach[A]                 = Foreach traversable xs
  def failEmpty(operation: String): Nothing              = throw new NoSuchElementException(s"$operation on empty collection")
  def index(x: Int): Index                               = Index(x)
  def indexRange(start: Int, end: Int): IndexRange       = IndexRange.until(Index(start), Index(end))
  def labelpf[T, R](label: String)(pf: T ?=> R): T ?=> R = new LabeledPartialFunction(pf, label)
  def loaderOf[A: ClassTag] : ClassLoader                = noNull(javaClassOf[A].getClassLoader, nullLoader)
  def errLog(msg: String): Unit                          = Console.err println msg
  def noNull[A](value: A, orElse: => A): A               = if (value == null) orElse else value
  def nth(x: Int): Nth                                   = Nth(x)
  def nullLoader(): ClassLoader                          = NullClassLoader
  def nullStream(): InputStream                          = NullInputStream
  def offset(x: Int): Offset                             = Offset(x)
  def resource(name: String): Array[Byte]                = Try(contextLoader) || loaderOf[this.type] fold (_ getResourceAsStream name slurp, _ => Array.empty)
  def resourceString(name: String): String               = fromUTF8(resource(name))
  def timed[A](body: => A): A                            = nanoTime |> (start => try body finally errLog("Elapsed: %.3f ms" format (nanoTime - start) / 1e6))
  def unknownSize: SizeInfo                              = SizeInfo.Unknown

  // OrderedMap is our own creation since SortedMap is way overspecified
  // and LinkedHashMap is too slow and only comes in a mutable variety.
  def orderedMap[K, V](kvs: (K, V)*): OrderedMap[K, V]                 = new OrderedMap[K, V](kvs map (_._1), kvs.toMap)
  def orderedMap[K, V](keys: Seq[K], map: Map[K, V]): OrderedMap[K, V] = new OrderedMap[K, V](keys, map)

  // Java.
  def jHashMap[K, V] : jHashMap[K, V] = new jHashMap[K, V]
  def jHashSet[A] : jHashSet[A]       = new jHashSet[A]
  def jList[A] : jArrayList[A]        = new jArrayList[A]
}
