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

import org.scalacheck._
import sbt.io._, syntax._

object FormatSpecification extends Properties("Format") {
  import Prop.forAll

  property("plainConversion") = forAll(nonDollar) { x => conversion(x, Map.empty[String, String]) == x }

  property("escapeDollar") = forAll(asciiString) { (x) => conversion("\\$" + x, Map.empty[String, String]) == "$" + x }

  property("formatUppercase") = forAll(asciiString, asciiString, nonDollar) { (x, y, z) =>
    conversion(s"""$$$x;format="upper"$$$z""", Map(x -> y)) == y.toUpperCase + z
  }

  property("formatLowercase") = forAll(asciiString, asciiString, nonDollar) { (x, y, z) =>
    conversion(s"""$$$x;format="lower"$$$z""", Map(x -> y)) == y.toLowerCase + z
  }

  property("formatDotReverse") = forAll(nonDotNonDollar, nonDotNonDollar, nonDotNonDollar) { (x, y, z) =>
    conversion("""$k;format="dot-reverse"$""", Map("k" -> s"$x.$y.$z")) == s"$z.$y.$x"
  }

  property("formatSnakecase") =
    conversion("""$x;format="snake"$""", Map("x" -> "My-Example-Project")) == "My_Example_Project"

  property("formatSpace") = conversion("""$x;format="space"$""", Map("x" -> "Foo-Bar_baz:_:bam")) == "Foo Bar baz bam"

  property("formatPackageNaming") = conversion("""$x;format="package"$""", Map("x" -> "foo bar  baz")) == "foo.bar.baz"

  prohibited_variable_names.map(x => property(s"$x is prohibited") = throws(s"$dollar$x$dollar", Map.empty))

  lazy val hiragana: List[Int] = (0x3041 to 0x3094).toList
  lazy val AZ: List[Int]       = (0x41 to 0x5a).toList
  lazy val az: List[Int]       = (0x61 to 0x7a).toList
  lazy val extAscii: List[Int] = (0x20 to 0xff).toList

  lazy val dollar: Char    = '$'
  lazy val backslash: Char = '\\'
  lazy val dot: Char       = '.'

  // These come from the expressions that is possible to use with ST4
  // https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
  lazy val prohibited_variable_names: List[String] = List(
    "i",
    "i0",
    "if",
    "else",
    "elseif",
    "endif",
    "first",
    "length",
    "strlen",
    "last",
    "rest",
    "reverse",
    "trunc",
    "strip",
    "trim"
  )

  lazy val nonDollarChar: Gen[Char] = Gen
    .oneOf(extAscii ::: hiragana)
    .filter(Character.isDefined)
    .map(_.toChar)
    .filter(x => x != dollar && x != backslash)
  lazy val nonDotNonDollarChar: Gen[Char] = nonDollarChar.filter(_ != dot)
  lazy val asciiChar: Gen[Char]           = Gen.oneOf(AZ ::: az).filter(Character.isDefined).map(_.toChar)

  lazy val nonDollar: Gen[String] = Gen
    .sized(Gen.listOfN(_, nonDollarChar))
    .map(_.mkString)
    .filter(_.nonEmpty)
    .filterNot(prohibited_variable_names.toSet)

  lazy val asciiString: Gen[String] = Gen
    .sized(Gen.listOfN(_, asciiChar))
    .map(_.mkString)
    .filter(_.nonEmpty)
    .filterNot(prohibited_variable_names.toSet)

  lazy val nonDotNonDollar: Gen[String] = Gen
    .sized(Gen.listOfN(_, nonDotNonDollarChar))
    .map(_.mkString)
    .filter(_.nonEmpty)
    .filterNot(prohibited_variable_names.toSet)

  def conversion(inContent: String, ps: Map[String, String]): String = synchronized {
    IO.withTemporaryDirectory { tempDir =>
      val in  = tempDir / "in.txt"
      val out = tempDir / "out.txt"
      IO.write(in, inContent, IO.utf8)
      G8(in, out, tempDir, ps)
      val outContent = IO.read(out, IO.utf8)
      outContent
    }
  }

  // if an expression is prohibited giter8 logs an STException
  // and the string returned by conversion is not replaced
  def throws(inContent: String, ps: Map[String, String]): Boolean = conversion(inContent, ps) == inContent
}
