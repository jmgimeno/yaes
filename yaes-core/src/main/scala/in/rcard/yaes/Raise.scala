package in.rcard.yaes

import scala.reflect.ClassTag
import scala.util.boundary
import scala.util.boundary.break
import scala.util.control.ControlThrowable
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal

/** An effect that represents the ability to raise an error of type `E`.
  *
  * Example usage:
  * {{{
  * trait ArithmeticError
  * case object DivisionByZero extends ArithmeticError
  * type DivisionByZero = DivisionByZero.type
  *
  * def divide(x: Int, y: Int)(using Raise[ArithmeticError]): Int =
  *   if (y == 0) then
  *     Raise.raise(DivisionByZero)
  *   else
  *     x / y
  *
  * // Using fold to handle errors
  * val result = Raise.fold {
  *   divide(10, 0)
  * } (onError = err => "Error: " + err)(onSuccess = res => "Result: " + res)
  * }}}
  *
  * This object contains various combinators and handlers for working with the Raise effect in a
  * safe and composable way.
  */
object Raise {

  type Raise[E] = Yaes[Raise.Unsafe[E]]

  /** Lifts a block of code that may use the Raise effect.
    *
    * @param block
    *   the code to execute
    * @return
    *   the result of the block if successful
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def apply[E, A](block: => A): Raise[E] ?=> A = block

  /** Raises an error in a context where a Raise effect is available.
    *
    * Example:
    * {{{
    * def ensurePositive(n: Int)(using Raise[String]): Int =
    *   if (n <= 0) then Raise.raise("Number must be positive")
    *   else n
    * }}}
    *
    * @param error
    *   the error to raise
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def raise[E, A](error: E)(using eff: Raise[E]): Nothing = eff.unsafe.raise(error)

  /** Handles both success and error cases of a computation that may raise an error.
    *
    * Example:
    * {{{
    * // Define our error type
    * sealed trait DivisionError
    * case object DivisionByZero extends DivisionError
    *
    * // Define a function that may raise an error
    * def divide(x: Int, y: Int)(using Raise[DivisionError]): Int =
    *   if (y == 0) then Raise.raise(DivisionByZero)
    *   else x / y
    *
    * // Handle both success and error cases using fold with curried parameters
    * val result = Raise.fold {
    *   divide(10, 0)
    * } {
    *   case DivisionByZero => "Cannot divide by zero"
    * } { result =>
    *   s"Result is $result"
    * }
    * // result will be "Cannot divide by zero"
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @param onError
    *   handler for the error case
    * @param onSuccess
    *   handler for the success case
    * @return
    *   the result of either onError or onSuccess
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    * @tparam B
    *   the type of the result of the handler
    */
  def fold[E, A, B](block: Raise[E] ?=> A)(onError: E => B)(onSuccess: A => B): B = {
    val handler = new Yaes.Handler[Raise.Unsafe[E], A, B] {

      override def handle(program: (Raise[E]) ?=> A): B = {
        boundary {
          given eff: Raise[E] = new Yaes(new Raise.Unsafe[E] {
            def raise(error: => E): Nothing =
              break(onError(error))
          })
          onSuccess(block)
        }
      }
    }
    Yaes.handle(block)(using handler)
  }

  /** Runs a computation that may raise an error and returns the result or the error.
    *
    * Example:
    * {{{
    * val result: Int | DivisionError = Raise.run {
    *   divide(10, 0)
    * }
    * // result will be DivisionError
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @return
    *   the result of the computation or the error
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def run[E, A](block: Raise[E] ?=> A): A | E = fold(block)(identity)(identity)

  /** Recovers from an error and returns a default value.
    *
    * Example:
    * {{{
    * val result = Raise.recover {
    *   divide(10, 0)
    * } {
    *   case DivisionByZero => "Cannot divide by zero"
    * }
    * // result will be "Cannot divide by zero"
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @param recoverWith
    *   the function to apply to the error
    * @return
    *   the result of the computation or the default value
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def recover[E, A](block: Raise[E] ?=> A)(recoverWith: E => A): A =
    fold(block)(onError = recoverWith)(onSuccess = identity)

  /** Returns the result of the computation or a default value if an error occurs.
    *
    * Example:
    * {{{
    * val result = Raise.withDefault(0) {
    *   divide(10, 0)
    * }
    * // result will be 0
    * }}}
    *
    * @param default
    *   the default value to return if an error occurs
    * @param block
    *   the computation that may raise an error
    * @return
    *   the result of the computation or the default value
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def withDefault[E, A](default: => A)(block: Raise[E] ?=> A): A = recover(block)(_ => default)

  /** Returns the result of the computation as an [[Either]].
    *
    * Example:
    * {{{
    * val result: Either[DivisionError, Int] = Raise.either {
    *   divide(10, 0)
    * }
    * // result will be Left(DivisionByZero)
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @return
    *   the result of the computation as an Either
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def either[E, A](block: Raise[E] ?=> A): Either[E, A] =
    fold(block)(onError = Left(_))(onSuccess = Right(_))

  /** Returns the result of the computation as an [[Option]].
    *
    * Example:
    * {{{
    * val result: Option[Int] = Raise.option {
    *   divide(10, 0)
    * }
    * // result will be None
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @return
    *   the result of the computation as an [[Option]]
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def option[E, A](block: Raise[E] ?=> A): Option[A] =
    fold(block)(onError_ => None)(onSuccess = Some(_))

  /** Returns the result of the computation as a nullable value.
    *
    * Example:
    * {{{
    * val result: Int | Null = Raise.nullable {
    *   divide(10, 0)
    * }
    * // result will be null
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @return
    *   the result of the computation as a nullable value
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def nullable[E, A](block: Raise[E] ?=> A): A | Null =
    fold(block)(onError_ => null)(onSuccess = identity)

  /** Ensures that a condition is true and raises an error if it is not.
    *
    * Example:
    * {{{
    * val num = 10
    * val result = Raise.run {
    *   Raise.ensure(num < 0)("Number must be positive")
    * }
    * // result will be "Number must be positive"
    * }}}
    *
    * @param condition
    *   the condition to ensure
    * @param error
    *   the error to raise if the condition is not met
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def ensure[E](condition: => Boolean)(error: => E)(using r: Raise[E]): Unit =
    if !condition then Raise.raise(error)

  /** Catches an exception and raises an error of type `E`. For other exceptions, the exception is
    * rethrown.
    *
    * Example:
    * {{{
    * val result = Raise.run {
    *   Raise.catching {
    *     10 / 0
    *   } {
    *     case ArithmeticException => DivisionByZero
    *   }
    * }
    * // result will be DivisionByZero
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @param mapException
    *   the function to apply to the exception
    * @return
    *   the result of the computation
    * @tparam E
    *   the type of error that can be raised
    * @tparam A
    *   the type of the result of the block
    */
  def catching[E, A](block: => A)(mapException: Throwable => E)(using r: Raise[E]): A =
    try {
      block
    } catch {
      case NonFatal(nfex) => Raise.raise(mapException(nfex))
      case ex             => throw ex
    }

  /** Catches an exception of type `E` and lifts it to an error. For other exceptions, the exception
    * is rethrown.
    *
    * Example:
    * {{{
    * val result: Int | ArithmeticException = Raise.run {
    *   Raise.catching[ArithmeticException] {
    *     10 / 0
    *   }
    * }
    * // result will be ArithmeticException
    * }}}
    *
    * @param block
    *   the computation that may raise an error
    * @tparam E
    *   the type of exception to catch and lift to an error
    * @tparam A
    *   the type of the result of the block
    */
  def catching[E <: Throwable, A](block: => A)(using r: Raise[E], E: ClassTag[E]): A =
    try {
      block
    } catch {
      case NonFatal(nfex) =>
        if (nfex.getClass == E.runtimeClass) Raise.raise(nfex.asInstanceOf[E])
        else throw nfex
      case ex => throw ex
    }

  /** An effect that represents the ability to raise an error of type `E`. */
  trait Unsafe[-E] {

    /** Raises an error of type `E`.
      *
      * @param error
      *   the error to raise
      */
    def raise(error: => E): Nothing
  }
}
