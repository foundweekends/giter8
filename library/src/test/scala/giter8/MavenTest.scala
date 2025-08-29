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

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class MavenTest extends AnyFlatSpec with Matchers with EitherValues {

  "Maven" should "resolve the latest version when found" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-compiler</artifactId>
        <versioning>
          <latest>2.13.17-M1</latest>
          <versions>
            <version>2.13.14</version>
            <version>2.13.15-M1</version>
            <version>2.13.15</version>
            <version>2.13.16</version>
            <version>2.13.17-M1</version>
          </versions>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/maven-metadata.xml"
    Maven.findLatestVersion(loc, xml).value should be("2.13.17-M1")
  }

  it should "return an error when the latest version is not found" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-compiler</artifactId>
        <versioning>
          <versions>
            <version>2.13.14</version>
            <version>2.13.15-M1</version>
            <version>2.13.15</version>
            <version>2.13.16</version>
            <version>2.13.17-M1</version>
          </versions>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/maven-metadata.xml"
    Maven.findLatestVersion(loc, xml).left.value should be(
      s"Found metadata at $loc but can't extract latest version"
    )
  }

  it should "resolve the latest stable version when the latest version is not stable" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-compiler</artifactId>
        <versioning>
          <versions>
            <version>2.13.14</version>
            <version>2.13.15-M1</version>
            <version>2.13.15</version>
            <version>2.13.16</version>
            <version>2.13.17-M1</version>
          </versions>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/maven-metadata.xml"
    Maven.findLatestStableVersion(loc, xml).value should be("2.13.16")
  }

  it should "return the latest version if the latest version is stable" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-compiler</artifactId>
        <versioning>
          <latest>2.13.16</latest>
          <versions>
            <version>2.13.14</version>
            <version>2.13.15-M1</version>
            <version>2.13.15</version>
            <version>2.13.16</version>
          </versions>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/maven-metadata.xml"
    Maven.findLatestStableVersion(loc, xml).value should be("2.13.16")
  }

  it should "return an error if the latest stable version is not found" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-compiler</artifactId>
        <versioning>
          <versions>
            <version>2.13.15-M1</version>
            <version>2.13.17-M1</version>
          </versions>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-compiler/maven-metadata.xml"
    Maven.findLatestStableVersion(loc, xml).left.value should be(
      s"Could not find latest stable version at $loc"
    )
  }
}
