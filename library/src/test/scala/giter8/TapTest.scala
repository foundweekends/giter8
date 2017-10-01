package giter8

import org.scalatest.{MustMatchers, WordSpec}

class TapTest extends WordSpec with MustMatchers {

  ".tap" must {

    "return itself" in {
      1.tap(_ => 2) mustEqual 1
    }

    "execute the block for its side effects" in {
      var tapped = false
      1.tap(_ => tapped = true)
      tapped mustEqual true
    }
  }
}
