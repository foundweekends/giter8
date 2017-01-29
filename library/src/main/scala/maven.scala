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

import scala.util.parsing.combinator._
import scala.xml.XML
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

// http://hc.apache.org/httpcomponents-client-4.2.x/httpclient/apidocs/

/**
 * Parse `maven-metadata.xml`
 */
object Maven extends JavaTokenParsers {
  private val org, name = """[\w\-\.]+""".r

  private val spec =
    "maven" ~> "(" ~> org ~ ("," ~> name) <~ ")" ^^ {
      case org ~ name =>
        (org, name)
    }

  private def unapply(value: String): Option[(String, String)] =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }

  private def latestVersion(
    org: String,
    name: String
  ): Either[String, String] = {
    val loc = s"https://repo1.maven.org/maven2/${org.replace('.', '/')}/$name/maven-metadata.xml"
    withHttp(loc) { response =>
      val status = response.getStatusLine
      status.getStatusCode match {
        case 200 =>
          val elem = XML.load(response.getEntity.getContent)
          (elem \ "versioning" \ "latest").headOption.map(
            elem => elem.text
          ).toRight(s"Found metadata at $loc but can't extract latest version")
        case 404 =>
          Left(s"Maven metadata not found for `maven($org, $name)`\nTried: $loc")
        case status =>
          Left(s"Unexpected response status $status fetching metadata from $loc")
      }
    }
  }

  def withHttp[A](url: String)(f: HttpResponse => A): A =
    {
      val httpClient = new DefaultHttpClient
      try {
        val r = new HttpGet(url)
        val response = httpClient.execute(r)
        try {
          f(response)
        } finally {
        }
      } finally {
      }
    }


  def lookup(rawDefaults: G8.OrderedProperties): Either[String, G8.OrderedProperties] = {
    val defaults = rawDefaults.map {
      case (k, Maven(org, name)) => k -> latestVersion(org, name)
      case (k, value)            => k -> Right(value)
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
