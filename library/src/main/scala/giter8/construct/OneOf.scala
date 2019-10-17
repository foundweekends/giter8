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
package construct

import atto._
import atto.Atto._
import cats.Show
import cats.data.NonEmptyList

final case class OneOf(possibilities: NonEmptyList[String]) {
  val description: String =
    s"one of ${possibilities.toList.mkString(", ")}, default ${possibilities.head}"

  def check(k: String, v: String): VersionE =
    if (possibilities.toList.contains(v)) Right(v)
    else Left(s"Parameter $k should be one of ${possibilities.toList.mkString(", ")}, " +
      s"was $v, default applied (${possibilities.head})")
}
object OneOf {
  implicit val showOneOf: Show[OneOf] =
    Show.show(o => s"oneOf(${o.possibilities.toList.mkString(", ")})")

  val parser: Parser[OneOf] = {
    val allowedChars = anyChar.filter(c => c != ',' && c != ')')
    val sepP = token(char(','))
    (string("oneOf") ~> parens(stringOf1(allowedChars).sepBy1(sepP)))
      .map(OneOf.apply)
      .namedOpaque("oneOf")
  }
}
