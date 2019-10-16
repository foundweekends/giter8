/*
 * Copyright 2017 by foundweekends project
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

import atto._
import atto.Atto._
import atto.syntax.refined._
import cats.Show
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

final case class Ls(owner: String, name: String)
object Ls {
  type ValidOwner = MatchesRegex[W.`"""[\\w\\-\\.]+"""`.T]
  type Owner = String Refined ValidOwner
  type ValidName = MatchesRegex[W.`"""[\\w\\-\\.]+"""`.T]
  type Name = String Refined ValidName

  implicit val showLs: Show[Ls] = Show.show(l => s"ls(${l.owner}, ${l.name})")

  val parser: Parser[Ls] = {
    val ownerP = stringOf1(letter).refined[ValidOwner].namedOpaque("owner")
    val nameP = stringOf1(letter).refined[ValidName].namedOpaque("name")
    val sepP = token(char(','))
    (for {
      _ <- string("ls") <~ char('(')
      owner <- ownerP <~ sepP
      name <- nameP
      _ <- char(')')
    } yield Ls(owner.value, name.value)).namedOpaque("ls")
  }

  def unapply(value: String): Option[(String, String)] =
    Ls.parser.parseOnly(value).option.map(ls => (ls.owner, ls.name))
}
