package pl.writeonly.addons.future.cats

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import pl.writeonly.addons.future.api.Ops.{GetOrFailed, InSideOut, TransRecover}
import pl.writeonly.addons.future.api.{EC, TypesBoth, Utils}

import scala.concurrent.Future

trait ValidatedNelFuture extends TypesBoth with Utils {

  override type Value[A, B] = ValidatedNel[A, B]

  override def inSideOut[A, B](
    v: FutureV[A, B]
  )(implicit ec: EC): ValueF[A, B] =
    v match {
      case Valid(f: Future[B]) => for (a <- f) yield Validated.valid(a)
      case a: Invalid[A]       => Future.successful(a)
    }

  override def getOrFailed[A, B](v: FutureV[A, B])(implicit ec: EC): Future[B] =
    v match {
      case Valid(f: Future[B]) => f
      case a: Invalid[NonEmptyList[A]] if a.e.size === 1 =>
        a.e.head |> toThrowable[A] |> Future.failed
      case a: Invalid[NonEmptyList[A]] =>
        a.e |> toThrowable[NonEmptyList[A]] |> Future.failed
    }

  override def transRecover[B](v: Future[B])(implicit ec: EC): RecoveredF[B] =
    v.transformAndRecover((s: B) => Valid(s), {
      case t => Validated.invalidNel(t)
    })

  //    value.transform({
  //      case Success(s) => Success(Good(s))
  //      case Failure(t) => Success(Bad(t))
  //    })

  implicit class ValidFutureInSideOut[A, B](v: FutureV[A, B])
      extends InSideOut[Value[A, B]] {
    override def inSideOut(implicit ec: EC): ValueF[A, B] =
      ValidatedNelFuture.inSideOut(v)(ec)
  }

  implicit class ValidFutureGetOrFailed[A, B](v: FutureV[A, B])
      extends GetOrFailed[B] {
    override def getOrFailed(implicit ec: EC): Future[B] =
      ValidatedNelFuture.getOrFailed(v)(ec)
  }

  implicit class ValidFutureTransRecover[B](v: Future[B])
      extends TransRecover[Recovered[B]] {
    override def transRecover(implicit ec: EC): RecoveredF[B] =
      ValidatedNelFuture.transRecover(v)(ec)
  }

}

object ValidatedNelFuture extends ValidatedNelFuture