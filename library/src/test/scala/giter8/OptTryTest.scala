package giter8

import org.scalatest.{MustMatchers, OptionValues, TryValues, WordSpec}

import scala.util.{Failure, Success, Try}

class OptTryTest extends WordSpec with MustMatchers with TryValues with OptionValues {

  object LeftFailure extends Throwable
  object RightFailure extends Throwable

  ".or" must {

    "return `true`" when {

      "`a` is a `Success(Some(true))`" when {

        val a = Success(Some(true))

        "`b` is a `Success(Some(false))" in {
          (a or Success(Some(false))).success.value.value mustEqual true
        }

        "`b` is a `Success(None)`" in {
          (a or Success(None)).success.value.value mustEqual true
        }

        "`b` is a `Failure`" in {
          (a or Failure(new Exception())).success.value.value mustEqual true
        }
      }

      "`a` is a `Success(None)`" when {

        val a: Try[Option[Boolean]] = Success(None)

        "`b` is a `Success(Some(true))`" in {
          (a or Success(Some(true))).success.value.value mustEqual true
        }
      }
    }

    "return `None`" when {

      "`a` and `b` are both `Success(None)`" in {
        (Success(None) or Success(None)).success.value mustNot be(defined)
      }
    }

    "return `Failure(LeftFailure)`" when {

      "`a` is a `Failure(LeftFailure)`" when {

        val a: Try[Option[Boolean]] = Failure(LeftFailure)

        "`b` is a `Failure(RightFailure)`" in {
          (a or Failure(RightFailure)).failure.exception mustEqual LeftFailure
        }

        "`b` is a `Success(None)`" in {
          (a or Success(None)).failure.exception mustEqual LeftFailure
        }

        "`b` is a `Success(Some(true))`" in {
          (a or Success(Some(true))).failure.exception mustEqual LeftFailure
        }
      }
    }

    "return `Failure(RightFailure)`" when {

      "`b` is a `Failure(RightFailure)`" when {

        val b: Try[Option[Boolean]] = Failure(RightFailure)

        "`a` is a `Success(None)`" in {
          val a: Try[Option[Boolean]] = Success(None)
          (a or b).failure.exception mustEqual RightFailure
        }
      }
    }
  }
}
