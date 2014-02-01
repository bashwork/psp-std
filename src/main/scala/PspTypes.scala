package psp
package core

import scala.{ collection => sc }
import sc.{ mutable => scm, generic => scg }

/** A thin abstraction over some questionable assumptions. */
trait PspTypes extends PspJavaTypes with PspScalaTypes {
  type Index              = Int
  type Done               = Boolean
  type Suspended[+A]      = (A => Unit) => Unit
  type Ref[+A]            = A with AnyRef
  type Predicate[-A]      = A => Boolean
  type Predicate2[-A, -B] = (A, B) => Boolean

  val MaxIndex       = Int.MaxValue
  val NoIndex        = -1
  val EOL            = sys.props.getOrElse("line.separator", "\n")

  type AtomicView[Repr, CC[X], A]  = ViewEnvironment[Repr, CC, A]#AtomicView
  type IndexedView[Repr, CC[X], A] = ViewEnvironment[Repr, CC, A]#IndexedView
  type LinearView[A, Repr, CC[X]]  = ViewEnvironment[Repr, CC, A]#LinearView

  type ForeachableType[Repr, CC0[X], A0] = Foreachable[Repr] {
    type A = A0
    type CC[B] = CC0[B]
  }
  type LinearableType[A0, Repr, CC0[X]] = Linearable[Repr] {
    type A = A0
    type CC[B] = CC0[B]
  }
  type IndexableType[Repr, CC0[X], A0] = Indexable[Repr] {
    type A = A0
    type CC[B] = CC0[B]
  }
}

trait PspJavaTypes {
  type jClass[A]              = java.lang.Class[A]
  type jHashSet[A]            = java.util.HashSet[A]
  type LinkedBlockingQueue[A] = java.util.concurrent.LinkedBlockingQueue[A]
  type BlockingQueue[A]       = java.util.concurrent.BlockingQueue[A]
  type SynchronousQueue[A]    = java.util.concurrent.SynchronousQueue[A]
}

trait PspScalaTypes {
  type tailrec      = scala.annotation.tailrec
  type uV           = scala.annotation.unchecked.uncheckedVariance
  type IdFun[A]     = A => A
  type =?> [-A, +B] = PartialFunction[A, B]

  type GenTraversableOnce[+A]          = sc.GenTraversableOnce[A]
  type Builder[-Elem, +To]             = scm.Builder[Elem, To]
  type WrappedArray[A]                 = scm.WrappedArray[A]
  type ArrayBuffer[A]                  = scm.ArrayBuffer[A]
  type CanBuildFrom[-From, -Elem, +To] = scg.CanBuildFrom[From, Elem, To]
  type ScalaNumber                     = scala.math.ScalaNumber
  type ClassTag[A]                     = scala.reflect.ClassTag[A]
}
