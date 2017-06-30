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

import java.io.File
import scala.util.parsing.combinator._
import scala.xml.{NodeSeq, XML}

/**
  * Parse `maven-metadata.xml`
  */
object Maven extends JavaTokenParsers with MavenHelper {
  private val org, name, release = """[\w\-\.]+""".r

  private val spec =
    "maven" ~> "(" ~> org ~ ("," ~> name) ~ (("," ~> release) ?) <~ ")" ^^ {
      case org ~ name ~ release => (org, name, release)
    }

  private def unapply(value: String): Option[(String, String, Option[String])] =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }

  private def mavenRepo(): Option[String] = {
    val home = Option(System.getProperty("G8_HOME"))
      .map(new File(_))
      .getOrElse(
        new File(System.getProperty("user.home"), ".g8")
      )
    val mvnRepo = new File(home + "/mvnrepo")
    if (!home.exists() || !mvnRepo.exists()) None
    else {
      scala.io.Source.fromFile(mvnRepo).getLines().find(!_.trim.isEmpty)
    }
  }

  private def latestVersion(
      org: String,
      name: String
  ): VersionE = fromMaven(org, name)(findLatestVersion)

  private def latestStableVersion(
      org: String,
      name: String
  ): VersionE = fromMaven(org, name)(findLatestStableVersion)

  private[giter8] def findLatestVersion(loc: String, elem: NodeSeq): VersionE = {
    (elem \ "versioning" \ "latest").headOption
      .map(_.text)
      .toRight(s"Found metadata at $loc but can't extract latest version")
  }

  private[giter8] def findLatestStableVersion(loc: String, elem: NodeSeq)(
      implicit svo: Ordering[VersionNumber]): VersionE = {
    (elem \ "versioning" \ "latest").headOption.map(_.text) match {
      case Some(VersionNumber.Stable(version)) => Right(version.toString)
      case _ =>
        val versions = (elem \ "versioning" \ "versions" \ "version").map(_.text)
        val validVersions = versions.collect {
          case VersionNumber.Stable(version) => version
        }
        validVersions.sorted.headOption.map(_.toString).toRight(s"Could not find latest stable version at $loc")
    }
  }

  def lsIsGone(artifact: String): VersionE =
    Left("ls() function is deprecated. " +
      s"Use maven() function to get latest version of ${artifact.trim}.")

  def lookup(rawDefaults: G8.OrderedProperties): Either[String, G8.OrderedProperties] = {
    val defaults = rawDefaults.map {
      case (k, Ls(owner, name))                  => k -> lsIsGone(name)
      case (k, Maven(org, name, Some("stable"))) => k -> latestStableVersion(org, name)
      case (k, Maven(org, name, _))              => k -> latestVersion(org, name)
      case (k, value)                            => k -> Right(value)
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
