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

object LsSpecification extends Properties("Ls") {
  implicit val oneOfGen: Arbitrary[Ls] = Arbitrary {
    for {
      owner <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
      name <- Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
    } yield Ls(owner, name)
  }

  property("roundtrip") = Prop.forAll { l: Ls =>
    Ls.parser.parseOnly(l.show).option == Some(l)
  }
}
