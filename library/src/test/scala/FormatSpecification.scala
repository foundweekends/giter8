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

  property("plainConversion") = forAll(nonDollar) { x =>
    conversion(x, Map.empty[String, String]) == x
  }

  property("escapeDollar") = forAll(asciiString) { (x) =>
    conversion("\\$" + x, Map.empty[String, String]) == "$" + x
  }

  property("formatUppercase") = forAll(asciiString, asciiString, nonDollar) { (x, y, z) =>
    conversion(s"""$$$x;format="upper"$$$z""", Map(x -> y)) == y.toUpperCase + z
  }

  property("formatLowercase") = forAll(asciiString, asciiString, nonDollar) { (x, y, z) =>
    conversion(s"""$$$x;format="lower"$$$z""", Map(x -> y)) == y.toLowerCase + z
  }

  property("formatSnakecase") =
    conversion("""$x;format="snake"$""", Map("x" -> "My-Example-Project")) == "My_Example_Project"

  property("formatWords") =
    conversion("""$x;format="words"$""", Map("x" -> "Foo-Bar_baz:_:bam")) == "Foo Bar baz bam"

  property("formatPackageNaming") =
    conversion("""$x;format="package"$""", Map("x" -> "foo bar  baz")) == "foo.bar.baz"

  lazy val hiragana = (0x3041 to 0x3094).toList

  lazy val nonDollarChar: Gen[Char] = Gen.oneOf(
    ((0x20 to 0xff).toList ::: hiragana).filter(x => Character.isDefined(x) && x != 0x24 && x != 0x5c).map(_.toChar))

  lazy val nonDollar: Gen[String] = Gen.sized { size =>
    Gen.listOfN(size, nonDollarChar).map(_.mkString)
  } filter { _.nonEmpty }

  lazy val asciiChar: Gen[Char] =
    Gen.oneOf(((0x41 to 0x5a).toList ::: (0x61 to 0x7a).toList).filter(x => Character.isDefined(x)).map(_.toChar))

  lazy val asciiString: Gen[String] = Gen.sized { size =>
    Gen.listOfN(size, asciiChar).map(_.mkString)
  } filter { _.nonEmpty }

  def conversion(inContent: String, ps: Map[String, String]): String = synchronized {
    IO.withTemporaryDirectory { tempDir =>
      val in  = tempDir / "in.txt"
      val out = tempDir / "out.txt"
      IO.write(in, inContent, IO.utf8)
      G8(in, out, tempDir, ps)
      val outContent = IO.read(out, IO.utf8)
      // println(outContent)
      outContent
    }
  }
}
