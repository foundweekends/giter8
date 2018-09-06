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

import org.scalatest.{EitherValues, FlatSpec, Matchers}

final class MavenTest extends FlatSpec with Matchers with EitherValues {

  "Maven" should "resolve the latest version when found" in {
    val xml =
      <response>
        <result name="response" numFound="1" start="0">
          <doc>
            <str name="g">org.scala-lang</str>
            <str name="id">org.scala-lang:scala-library</str>
            <str name="latestVersion">2.13.0-M5</str>
          </doc>
        </result>
      </response>

    val loc =
      "https://search.maven.org/solrsearch/select?q=g:%22org.scala-lang%22+AND+a:%22scala-library%22&rows=20&wt=xml"
    Maven.findLatestVersion(loc, xml).right.value should be("2.13.0-M5")
  }

  it should "return an error when the latest version is not found" in {
    val xml =
      <response>
        <result name="response" numFound="1" start="0">
          <doc>
            <str name="g">org.scala-lang</str>
            <str name="id">org.scala-lang:scala-library</str>
          </doc>
        </result>
      </response>

    val loc =
      "https://search.maven.org/solrsearch/select?q=g:%22org.scala-lang%22+AND+a:%22scala-library%22&rows=20&wt=xml"
    Maven.findLatestVersion(loc, xml).left.value should be(
      s"Found metadata at $loc but can't extract latest version"
    )
  }

  it should "resolve the latest stable version when the latest version is not stable" in {
    val xml =
      <response>
        <result name="response" numFound="21" start="0">
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.0.6-SNAP2</str>
            <str name="v">3.0.6-SNAP2</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.0.6-SNAP1</str>
            <str name="v">3.0.6-SNAP1</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.0.5-M1</str>
            <str name="v">3.0.5-M1</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.0.5</str>
            <str name="v">3.0.5</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.2.0-SNAP10</str>
            <str name="v">3.2.0-SNAP10</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.1.0-SNAP6</str>
            <str name="v">3.1.0-SNAP6</str>
          </doc>
          <doc>
            <str name="a">scalatest_2.12</str>
            <str name="g">org.scalatest</str>
            <str name="id">org.scalatest:scalatest_2.12:3.0.4</str>
            <str name="v">3.0.4</str>
          </doc>
        </result>
      </response>

    val loc =
      "https://search.maven.org/solrsearch/select?q=g:%22org.scalatest%22+AND+a:%22scalatest_2.12%22&rows=20&wt=xml&core=gav"
    Maven.findLatestStableVersion(loc, xml).right.value should be("3.0.5")
  }

  it should "return the latest version if the latest version is stable" in {
    val xml =
      <response>
        <result name="response" numFound="5" start="0">
          <doc>
            <str name="a">scalacheck_2.12</str>
            <str name="g">org.scalacheck</str>
            <str name="id">org.scalacheck:scalacheck_2.12:1.14.0</str>
            <str name="v">1.14.0</str>
          </doc>
          <doc>
            <str name="a">scalacheck_2.12</str>
            <str name="g">org.scalacheck</str>
            <str name="id">org.scalacheck:scalacheck_2.12:1.13.5</str>
            <str name="v">1.13.5</str>
          </doc>
          <doc>
            <str name="a">scalacheck_2.12</str>
            <str name="g">org.scalacheck</str>
            <str name="id">org.scalacheck:scalacheck_2.12:1.12.6</str>
            <str name="v">1.12.6</str>
          </doc>
          <doc>
            <str name="a">scalacheck_2.12</str>
            <str name="g">org.scalacheck</str>
            <str name="id">org.scalacheck:scalacheck_2.12:1.13.4</str>
            <str name="v">1.13.4</str>
          </doc>
          <doc>
            <str name="a">scalacheck_2.12</str>
            <str name="g">org.scalacheck</str>
            <str name="id">org.scalacheck:scalacheck_2.12:1.11.6</str>
            <str name="p">jar</str>
            <str name="v">1.11.6</str>
          </doc>
        </result>
      </response>

    val loc =
      "https://search.maven.org/solrsearch/select?q=g:%22org.scalacheck%22+AND+a:%22scalacheck_2.12%22&core=gav&rows=20&wt=xml"
    Maven.findLatestStableVersion(loc, xml).right.value should be("1.14.0")
  }

  it should "return an error if the latest stable version is not found" in {
    val xml =
      <response>
        <result name="response" numFound="47" start="0">
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M24</str>
            <str name="v">7.3.0-M24</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M23</str>
            <str name="v">7.3.0-M23</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M22</str>
            <str name="v">7.3.0-M22</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M21</str>
            <str name="v">7.3.0-M21</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M20</str>
            <str name="v">7.3.0-M20</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M19</str>
            <str name="v">7.3.0-M19</str>
          </doc>
          <doc>
            <str name="a">scalaz-concurrent_2.12</str>
            <str name="g">org.scalaz</str>
            <str name="id">org.scalaz:scalaz-concurrent_2.12:7.3.0-M18</str>
            <long name="timestamp">1509498138000</long>
            <str name="v">7.3.0-M18</str>
          </doc>
        </result>
      </response>

    val loc =
      "https://search.maven.org/solrsearch/select?q=g:%22org.scalaz%22+AND+a:%22scalaz-concurrent_2.12%22&rows=20&wt=xml&core=gav"
    Maven.findLatestStableVersion(loc, xml).left.value should be(
      s"Could not find latest stable version at $loc"
    )
  }
}
