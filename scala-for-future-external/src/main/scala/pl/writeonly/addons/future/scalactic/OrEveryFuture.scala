package pl.writeonly.addons.future.scalactic

import org.scalactic._
import pl.writeonly.addons.future.api.Ops.{GetOrFailed, InSideOut, TransRecover}
import pl.writeonly.addons.future.api.{EC, TypesBoth, Utils}

import scala.concurrent.Future

trait OrEveryFuture extends TypesBoth with Utils {

  override type Value[A, B] = B Or Every[A]

  override def getOrFailed[A, B](v: FutureV[A, B])(implicit ec: EC): Future[B] =
    v match {
      case Good(f: Future[B]) => f
      case Bad(f: A)          => f |> toThrowable[A] |> Future.failed
    }

  override def inSideOut[A, B](
    v: FutureV[A, B]
  )(implicit ec: EC): ValueF[A, B] =
    v match {
      case Good(f: Future[B]) => for (a <- f) yield Good(a)
      case a @ Bad(_)         => a |> Future.successful
    }

//  override def recover[A](v: Future[A])(implicit ec: EC): Recovered[A] = ???

  def transRecover[A](v: Future[A])(implicit ec: EC): RecoveredF[A] =
    v.transformAndRecover((s: A) => Good(s), { case t => Bad(One(t)) })

  implicit class OrEveryFutureInSideOut[B, A](v: FutureV[A, B])
      extends InSideOut[Value[A, B]] {
    override def inSideOut(implicit ec: EC): ValueF[A, B] =
      OrEveryFuture.inSideOut[A, B](v)(ec)
  }

  implicit class OrEveryFutureGetOrFailed[A, B](v: FutureV[A, B])
      extends GetOrFailed[B] {
    override def getOrFailed(implicit ec: EC): Future[B] =
      OrEveryFuture.getOrFailed(v)(ec)
  }

  implicit class OrEveryFutureTransRecover[A](value: Future[A])
      extends TransRecover[Recovered[A]] {

    override def transRecover(implicit ec: EC): RecoveredF[A] =
      OrEveryFuture.transRecover(value)(ec)
  }

}

object OrEveryFuture extends OrEveryFuture