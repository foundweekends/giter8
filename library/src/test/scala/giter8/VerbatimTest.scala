package giter8

import java.io.File

import org.scalatest.{FlatSpec, Matchers}


class VerbatimTest extends FlatSpec with Matchers {

  "G8" should "ignore file based on extension" in {
    G8.verbatim(new File("/myProject/public/fr/index.html"), Map("verbatim" -> "*.html"), new File("/")) shouldBe true
  }

  it should "ignore file with wildcard referring a partial path" in {
    G8.verbatim(new File("/myProject/public/fr/index.html"), Map("verbatim" -> "*/public/*"), new File("/")) shouldBe true
    G8.verbatim(new File("/myProject/public/fr/index.html"), Map("verbatim" -> "/myProject/**/*.html"), new File("/")) shouldBe true
  }
}
