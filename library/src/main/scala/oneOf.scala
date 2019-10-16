/*
 * Original implementation (C) 2014-2015 Kenji Yoshida and contributors
 * Adapted and extended in 2016 by foundweekends project
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

import atto._
import atto.Atto._
import cats.Show
import cats.data.NonEmptyList

final case class OneOf(possibilities: NonEmptyList[String])
object OneOf {
  implicit val showOneOf: Show[OneOf] =
    Show.show(o => s"oneOf(${o.possibilities.toList.mkString(", ")})")

  val parser: Parser[OneOf] = {
    val sepParser = token(char(','))
    (string("oneOf") ~> parens(stringOf1(letter).sepBy1(sepParser)))
      .map(OneOf.apply)
      .namedOpaque("oneOf")
  }
}
