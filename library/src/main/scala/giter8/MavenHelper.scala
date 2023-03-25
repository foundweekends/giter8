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

import java.net.{HttpURLConnection, URI, URL}
import scala.xml.{NodeSeq, XML}

trait MavenHelper {
  def fromMaven(org: String, name: String, getVersions: Boolean)(process: (String, NodeSeq) => VersionE): VersionE = {
    val search = "https://search.maven.org/solrsearch/select"
    val loc    = s"""$search?q=g:%22$org%22+AND+a:%22$name%22&rows=10&wt=xml${if (getVersions) "&core=gav" else ""}"""

    withHttp(new URI(loc).toURL) { conn =>
      conn.getResponseCode match {
        case 200 =>
          val elem = XML.load(conn.getInputStream)
          process(loc, elem)
        case 404 =>
          Left(s"Maven metadata not found for `maven($org, $name)`\nTried: $loc")
        case status =>
          Left(s"Unexpected response status $status fetching metadata from $loc")
      }
    }
  }

  def withHttp[A](url: URL)(f: HttpURLConnection => A): A = {
    val conn = url.openConnection()
    try {
      conn match {
        case httpConn: HttpURLConnection => f(httpConn)
      }
    } finally {
      conn match {
        case httpConn: HttpURLConnection => httpConn.disconnect()
      }
    }
  }
}
