/*
 * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
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

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.{Failure, Success, Try}
import scala.xml.XML

case class HttpGetRequest(url: String)
case class HttpResponse(code: Int, status: String, body: Option[String])

trait HttpClient {
  def execute(request: HttpGetRequest): Try[HttpResponse]
}

object Maven extends JavaTokenParsers {
  private val org, name, release = """[\w\-\.]+""".r

  private val spec =
    "maven" ~> "(" ~> org ~ ("," ~> name) ~ ("," ~> release).? <~ ")" ^^ {
      case org ~ name ~ release => (org, name, release)
    }

  def unapply(value: String): Option[(String, String, Option[String])] =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }
}

case class MavenPropertyResolver(httpClient: HttpClient) extends PropertyResolver {
  import MavenPropertyResolver._

  override protected def resolveImpl(old: Map[String, String]): Try[Map[String, String]] = Try {
    old.map {
      case (name, value) =>
        value match {
          case Maven(groupId, artifactId, Some("stable")) =>
            resolveFromMaven(groupId.trim, artifactId.trim, stable = true) match {
              case Success(version) => name -> version
              case Failure(t)       => throw t
            }
          case Maven(groupId, artifactId, _) =>
            resolveFromMaven(groupId.trim, artifactId.trim, stable = false) match {
              case Success(version) => name -> version
              case Failure(t)       => throw t
            }
          case _ => name -> value
        }
    }
  }

  private def resolveFromMaven(groupId: String, artifactId: String, stable: Boolean): Try[String] = {
    val repository = mavenRepo.getOrElse(DEFAULT_MAVEN_REPOSITORY)

    val url = s"$repository/${groupId.replace('.', '/')}/$artifactId/maven-metadata.xml"
    withHttp(url) { response =>
      response.code match {
        case 200 =>
          if (stable) {
            response.body.flatMap(extractLatestStableVersion) match {
              case None          => Failure(MavenError(s"Found metadata at $url but can't extract latest stable version"))
              case Some(version) => Success(version)
            }
          } else {
            response.body.flatMap(extractLatestVersion) match {
              case None          => Failure(MavenError(s"Found metadata at $url but can't extract latest version"))
              case Some(version) => Success(version)
            }
          }
        case 404 => Failure(MavenError(s"Maven metadata not found for `maven($groupId, $artifactId)`\nTried: $url"))
        case _ =>
          val statusLine = s"${response.code} ${response.status}"
          Failure(MavenError(s"Unexpected response status $statusLine fetching metadata from $url"))
      }
    }
  }

  private def mavenRepo: Option[String] = {
    val homeStr = System.getProperty("G8_HOME", System.getProperty("user.home") + File.separator + ".g8")
    val home    = new File(homeStr)
    val mvnRepo = new File(homeStr + File.separator + "mvnrepo")
    if (!home.exists() || !mvnRepo.exists()) None
    else {
      scala.io.Source.fromFile(mvnRepo).getLines().find(!_.trim.isEmpty)
    }
  }

  private def extractLatestVersion(input: String): Option[String] = {
    val xml           = XML.loadString(input)
    val latestVersion = (xml \ "versioning" \ "latest").headOption
    latestVersion.map(_.text)
  }

  private def extractLatestStableVersion(input: String): Option[String] = {
    val xml           = XML.loadString(input)
    val latestVersion = (xml \ "versioning" \ "latest").headOption
    latestVersion.map(_.text) match {
      case Some(VersionNumber.Stable(version)) => Some(version.toString)
      case _ =>
        val versions = (xml \ "versioning" \ "versions" \ "version").map(_.text)
        val validVersions = versions.collect {
          case VersionNumber.Stable(version) => version
        }
        validVersions.sorted.headOption.map(_.toString)
    }
  }

  private def withHttp[A](url: String)(f: HttpResponse => Try[A]): Try[A] = {
    for {
      request  <- Try(HttpGetRequest(url))
      response <- httpClient.execute(request)
      result   <- f(response)
    } yield result
  }
}

object MavenPropertyResolver {
  val DEFAULT_MAVEN_REPOSITORY = "https://repo1.maven.org/maven2"

  case class MavenError(message: String) extends PropertyResolver.PropertyResolvingError(message)
}
