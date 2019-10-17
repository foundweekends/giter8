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

package giter8.construct

import atto.Atto._
import cats.syntax.show._
import org.scalacheck._

object MavenSpecification extends Properties("Maven") {
  val orgGen: Gen[String] = Gen
    .nonEmptyListOf(
      Gen.oneOf(
        Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
        Gen.const(".")
      )
    )
    .map(_.mkString)
  val nameGen: Gen[String] = for {
    n <- Gen
      .nonEmptyListOf(
        Gen.oneOf(
          Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString),
          Gen.const("-")
        )
      )
      .map(_.mkString)
    v <- Gen.oneOf("_2.11", "_2.12", "_2.13")
  } yield n + v
  implicit val mavenGen: Arbitrary[Maven] = Arbitrary {
    for {
      org    <- orgGen
      name   <- nameGen
      stable <- Gen.oneOf(true, false)
    } yield Maven(org, name, stable)
  }

  property("roundtrip") = Prop.forAll { m: Maven =>
    Maven.parser.parseOnly(m.show).option == Some(m)
  }
}
