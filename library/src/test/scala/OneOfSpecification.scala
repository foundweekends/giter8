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

import atto.Atto._
import cats.data.NonEmptyList
import cats.syntax.show._
import org.scalacheck._

object OneOfSpecification extends Properties("OneOf") {
  implicit val oneOfGen: Arbitrary[OneOf] = Arbitrary {
    Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString))
      .map(ls => OneOf(NonEmptyList.fromListUnsafe(ls)))
  }

  property("OneOf roundtrip") = Prop.forAll { o: OneOf =>
    OneOf.parser.parseOnly(o.show).option == Some(o)
  }
}
