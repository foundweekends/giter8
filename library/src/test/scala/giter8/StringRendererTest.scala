/*
 * Original implementation (C) 2016 Eugene Yokota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package giter8

import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.util.{Failure, Success}

class StringRendererTest extends FlatSpec with Matchers with TryValues {
  import StringRendererTest._
  import StringRenderer._

  "StringRenderer" should "substitute parameters with correct values" in {
    "$foo$" withParameters Map("foo" -> "bar") shouldBe formattedAs("bar")
    "$baz$" withParameters Map("baz" -> "quux", "foo" -> "bar") shouldBe formattedAs("quux")
  }

  it can "format parameter value in uppercase" in {
    "$foo;format=\"upper\"$" withParameters Map("foo" -> "bar") shouldBe formattedAs("BAR")
    "$foo;format=\"upper\"$" withParameters Map("foo" -> "bar-bar") shouldBe formattedAs("BAR-BAR")

    "$foo;format=\"uppercase\"$" withParameters Map("foo" -> "bar") shouldBe formattedAs("BAR")

    "$foo;format=\"upper\"$" withParameters Map("foo" -> "My awesome parameter") shouldBe
      formattedAs("MY AWESOME PARAMETER")
  }

  it can "format parameter value in lowercase" in {
    "$foo;format=\"lower\"$" withParameters Map("foo" -> "BAR") shouldBe formattedAs("bar")
    "$foo;format=\"lower\"$" withParameters Map("foo" -> "BaR") shouldBe formattedAs("bar")
    "$foo;format=\"lower\"$" withParameters Map("foo" -> "AWESOME parameter") shouldBe formattedAs("awesome parameter")

    "$foo;format=\"lowercase\"$" withParameters Map("foo" -> "Bar-BaR") shouldBe formattedAs("bar-bar")
  }

  it can "capitalize first word of parameter value" in {
    "$foo;format=\"cap\"$" withParameters Map("foo" -> "bar") shouldBe formattedAs("Bar")
    "$foo;format=\"cap\"$" withParameters Map("foo" -> "lisp-case") shouldBe formattedAs("Lisp-case")

    "$foo;format=\"capitalize\"$" withParameters Map("foo" -> "my awesome foo") shouldBe formattedAs("My awesome foo")
  }

  it can "decapitalize first word of parameter value" in {
    "$foo;format=\"decap\"$" withParameters Map("foo" -> "Bar") shouldBe formattedAs("bar")

    "$foo;format=\"decapitalize\"$" withParameters Map("foo" -> "BAR") shouldBe formattedAs("bAR")

    "$foo;format=\"decap\"$" withParameters Map("foo" -> "A Capitalized Parameter") shouldBe
      formattedAs("a Capitalized Parameter")
  }

  it can "capitalize all parameter" in {
    "$foo;format=\"start\"$" withParameters Map("foo" -> "bar") shouldBe formattedAs("Bar")

    "$foo;format=\"start-case\"$" withParameters Map("foo" -> "foo bar") shouldBe formattedAs("Foo Bar")
    "$foo;format=\"start-case\"$" withParameters Map("foo" -> "my-awesome-parameter") shouldBe
      formattedAs("My-awesome-parameter")
  }

  it can "remove all non-word characters" in {
    "$foo;format=\"word\"$" withParameters Map("foo" -> "bar-!#%&foo") shouldBe formattedAs("barfoo")

    "$foo;format=\"word-only\"$" withParameters Map("foo" -> "b@#!@ar") shouldBe formattedAs("bar")
  }

  it can "format parameter value in UpperCamelCase" in {
    "$foo;format=\"Camel\"$" withParameters Map("foo" -> "with spaces") shouldBe formattedAs("WithSpaces")

    // TODO: fix upper camel case
    "$foo;format=\"Camel\"$" withParameters Map("foo" -> "foo-bar") shouldBe formattedAs("Foobar")

    "$foo;format=\"upper-camel\"$" withParameters Map("foo" -> "under_score") shouldBe formattedAs("Under_score")
  }

  it can "format parameter value in lowerCamelCase" in {
    "$foo;format=\"camel\"$" withParameters Map("foo" -> "with spaces") shouldBe formattedAs("withSpaces")

    // TODO: fix lower camel case
    "$foo;format=\"lower-camel\"$" withParameters Map("foo" -> "under_scored") shouldBe formattedAs("under_scored")
  }

  it can "hyphenate parameter value" in {
    "$foo;format=\"hyphen\"$" withParameters Map("foo" -> "foo bar") shouldBe formattedAs("foo-bar")

    // TODO: fix hyphenation
    "$foo;format=\"hyphenate\"$" withParameters Map("foo" -> "with_underscore") shouldBe formattedAs("with_underscore")
  }

  it can "normalize parameter value" in {
    "$foo;format=\"norm\"$" withParameters Map("foo" -> "My Awesome Project") shouldBe
      formattedAs("my-awesome-project")

    "$foo;format=\"normalize\"$" withParameters Map("foo" -> "My Awesome Project") shouldBe
      formattedAs("my-awesome-project")
  }

  it can "format parameter value in snake-case" in {
    "$foo;format=\"snake\"$" withParameters Map("foo" -> "with spaces") shouldBe formattedAs("with_spaces")

    "$foo;format=\"snake-case\"$" withParameters Map("foo" -> "with-dash") shouldBe formattedAs("with_dash")
  }

  it can "package" in {
    "$foo;format=\"packaged\"$" withParameters Map("foo" -> "com.example") shouldBe formattedAs("com/example")

    "$foo;format=\"package-dir\"$" withParameters Map("foo" -> "com.example.foo") shouldBe
      formattedAs("com/example/foo")
  }

  it can "add random id to parameter value" in {
    render("$foo;format=\"random\"$", Map("foo" -> "bar")).success.value should fullyMatch regex "bar-.*".r

    render("$foo;format=\"generate-random\"$", Map("foo" -> "bar")).success.value should fullyMatch regex "bar-.*".r
  }

  it should "return ParameterNotFoundError if parameter is not found" in {
    render("$foo$", Map.empty).failure.exception shouldBe a[ParameterNotFoundError]
  }

  ignore should "return IncorrectFormatError if format argument is incorrect" in {
    render("$foo;format=\"ololo\"$", Map("foo" -> "bar")).failure.exception shouldBe a[IncorrectFormatError]
  }
}

object StringRendererTest {
  implicit class StringExtension(string: String) {
    def withParameters(parameters: Map[String, String]) = BodyWithParameters(string, parameters)
  }

  case class BodyWithParameters(body: String, parameters: Map[String, String])

  def formattedAs(right: String) = new BeMatcher[BodyWithParameters] {
    override def apply(left: BodyWithParameters): MatchResult = {
      StringRenderer.render(left.body, left.parameters) match {
        case Success(actual) =>
          MatchResult(
            matches                  = actual == right,
            rawNegatedFailureMessage = s""""${left.body}" was formatted as "$right"""",
            rawFailureMessage = s""""${left.body}" should be formatted as "$right" with """ +
                s"""${formatParameters(left.parameters)}, but got "$actual".""".stripMargin
          )
        case Failure(e) =>
          MatchResult(
            matches                  = false,
            rawFailureMessage        = s"""Failed rendering string "${left.body}": ${e.getMessage}""",
            rawNegatedFailureMessage = "unreachable"
          )
      }
    }
  }

  private def formatParameters(parameters: Map[String, String]) = {
    if (parameters.isEmpty) "empty parameters"
    else s"parameters [${makeParametersList(parameters).mkString(",")}]"
  }

  private def makeParametersList(parameters: Map[String, String]) = parameters.toSeq.map {
    case (k, v) => s"$k=$v"
  }
}
