package giter8

import org.scalatest.{EitherValues, FlatSpec, Matchers}

final class MavenTest extends FlatSpec with Matchers with EitherValues {

  "Maven" should "resolve the latest version when found" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-library</artifactId>
        <versioning>
          <latest>2.12.1</latest>
          <release>2.12.1</release>
        </versioning>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-libary/maven-metadata.xml"
    Maven.findLatestVersion(loc, xml).right.value should be ("2.12.1")
  }

  it should "return an error when the latest version is not found" in {
    val xml =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-library</artifactId>
      </metadata>

    val loc = "https://repo1.maven.org/maven2/org/scala-lang/scala-libary/maven-metadata.xml"
    Maven.findLatestVersion(loc, xml).left.value should be (
      s"Found metadata at $loc but can't extract latest version"
    )
  }

  it should "resolve the latest stable version when the latest version is not stable" in {
    val xml =
      <metadata>
        <groupId>org.scalatest</groupId>
        <artifactId>scalatest_2.12</artifactId>
        <versioning>
          <latest>3.2.0-SNAP4</latest>
          <release>3.2.0-SNAP4</release>
          <versions>
            <version>3.0.0</version>
            <version>3.0.1</version>
            <version>3.2.0-SNAP1</version>
            <version>3.2.0-SNAP2</version>
            <version>3.2.0-SNAP3</version>
            <version>3.2.0-SNAP4</version>
          </versions>
          <lastUpdated>20170217001002</lastUpdated>
          </versioning>
      </metadata>

      val loc = "https://repo1.maven.org/maven2/org/scalatest/scalatest_2.12/maven-metadata.xml"
      Maven.findLatestStableVersion(loc, xml).right.value should be ("3.0.1")
  }

  it should "return the latest version if the latest version is stable" in {
    val xml =
      <metadata>
      <groupId>org.scalacheck</groupId>
      <artifactId>scalacheck_2.12</artifactId>
        <versioning>
          <latest>1.13.5</latest>
          <release>1.13.5</release>
          <versions>
          <version>1.11.6</version>
          <version>1.12.6</version>
          <version>1.13.4</version>
          <version>1.13.5</version>
        </versions>
        <lastUpdated>20170313115803</lastUpdated>
      </versioning>
      </metadata>

      val loc = "https://repo1.maven.org/maven2/org/scalacheck/scalacheck_2.12/maven-metadata.xml"
      Maven.findLatestStableVersion(loc, xml).right.value should be ("1.13.5")
  }

  it should "return an error if the latest stable version is not found" in {
    val xml =
      <metadata>
        <groupId>org.scalaz</groupId>
        <artifactId>scalaz-concurrent_2.12</artifactId>
        <versioning>
          <latest>7.3.0-M10</latest>
          <release>7.3.0-M10</release>
        <versions>
          <version>7.3.0-M6</version>
          <version>7.3.0-M7</version>
          <version>7.3.0-M8</version>
          <version>7.3.0-M9</version>
          <version>7.3.0-M10</version>
        </versions>
        <lastUpdated>20170319035300</lastUpdated>
      </versioning>
      </metadata>

      val loc = "https://repo1.maven.org/maven2/org/scalaz/scalaz-concurrent_2.12/maven-metadata.xml"
      Maven.findLatestStableVersion(loc, xml).left.value should be (
        s"Could not find latest stable version at $loc"
      )
  }
}