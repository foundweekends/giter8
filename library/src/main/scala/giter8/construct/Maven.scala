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
import atto.syntax.refined._
import cats.Show
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import scala.xml.NodeSeq

final case class Maven(org: String, name: String, stable: Boolean)
object Maven extends utils.MavenHelper {
  type ValidOrg  = MatchesRegex[W.`"""[\\w\\-\\.]+"""`.T]
  type ValidName = MatchesRegex[W.`"""[\\w\\-\\.]+"""`.T]

  implicit val showMaven: Show[Maven] = Show.show { m =>
    s"maven(${m.org}, ${m.name}${if (m.stable) ", stable" else ""})"
  }

  val parser: Parser[Maven] = {
    val allowedChars = letter | digit | char('_') | char('-') | char('.')
    val orgP         = stringOf1(allowedChars).refined[ValidOrg].namedOpaque("org")
    val nameP        = stringOf1(allowedChars).refined[ValidName].namedOpaque("name")
    val sepP         = token(char(','))
    (for {
      _      <- string("maven") <~ char('(')
      org    <- orgP <~ sepP
      name   <- nameP
      stable <- opt(sepP ~> string("stable"))
      _      <- char(')')
    } yield Maven(org.value, name.value, stable.isDefined)).namedOpaque("maven")
  }

  private def unapply(value: String): Option[(String, String, Boolean)] =
    parser.parseOnly(value).option.map(mvn => (mvn.org, mvn.name, mvn.stable))

  private def latestVersion(
      org: String,
      name: String
  ): VersionE = fromMaven(org, name, false)(findLatestVersion)

  private def latestStableVersion(
      org: String,
      name: String
  ): VersionE = fromMaven(org, name, true)(findLatestStableVersion)

  private[giter8] def findLatestVersion(loc: String, elem: NodeSeq): VersionE =
    (elem \ "result" \ "doc" \ "str")
      .collectFirst { case x if x.attribute("name").map(_.text) == Some("latestVersion") => x.text }
      .toRight(s"Found metadata at $loc but can't extract latest version")

  private[giter8] def findLatestStableVersion(loc: String, elem: NodeSeq)(
      implicit svo: Ordering[VersionNumber]
  ): VersionE = {
    val versions = (elem \ "result" \ "doc" \ "str").collect {
      case x if x.attribute("name").map(_.text) == Some("v") => x.text
    }
    val validVersions = versions.collect {
      case VersionNumber.Stable(version) => version
    }
    validVersions.sorted.headOption.map(_.toString).toRight(s"Could not find latest stable version at $loc")
  }

  def lsIsGone(artifact: String): VersionE =
    Left(
      "ls() function is deprecated. " +
        s"Use maven() function to get latest version of ${artifact.trim}."
    )

  def lookup(rawDefaults: G8.OrderedProperties): Either[String, G8.OrderedProperties] = {
    val defaults = rawDefaults.map {
      case (k, Ls(owner, name))        => k -> lsIsGone(name)
      case (k, Maven(org, name, true)) => k -> latestStableVersion(org, name)
      case (k, Maven(org, name, _))    => k -> latestVersion(org, name)
      case (k, value)                  => k -> Right(value)
    }
    val initial: Either[String, G8.OrderedProperties] = Right(List.empty)
    defaults.reverseIterator.foldLeft(initial) {
      case (accumEither, (k, either)) =>
        for {
          cur   <- accumEither.right
          value <- either.right
        } yield (k -> value) :: cur
    }
  }
}
