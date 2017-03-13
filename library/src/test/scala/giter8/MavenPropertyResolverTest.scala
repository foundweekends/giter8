package giter8

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.util.{Success, Try}
import scala.xml.Elem

class MavenPropertyResolverTest extends FlatSpec with TryValues with Matchers with MockFactory {

  "MavenProperty resolver" should "resolve properties like maven(groupId, artifactId) from Maven" in {
    val propertyValue = "maven(org.scala-lang, scala-library)"
    val url           = "https://repo1.maven.org/maven2/org/scala-lang/scala-library/maven-metadata.xml"
    val response =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-library</artifactId>
        <versioning>
          <latest>2.12.1</latest>
          <release>2.12.1</release>
        </versioning>
      </metadata>

    resolveMavenArtifactVersion(propertyValue, url, Some(response)).success.value shouldEqual "2.12.1"
  }

  it should "return a MavenError if metadata is mot available" in {
    val url           = "https://repo1.maven.org/maven2/com/foo/bar_2.11/maven-metadata.xml"
    val propertyValue = "maven(com.foo, bar_2.11)"

    val failure = resolveMavenArtifactVersion(propertyValue, url, None).failure
    failure.exception shouldBe a[MavenPropertyResolver.MavenError]
  }

  it should "return an error when the latest version is not found" in {
    val propertyValue = "maven(org.scala-lang, scala-library)"
    val url           = "https://repo1.maven.org/maven2/org/scala-lang/scala-library/maven-metadata.xml"
    val response =
      <metadata>
        <groupId>org.scala-lang</groupId>
        <artifactId>scala-library</artifactId>
      </metadata>

    val failure = resolveMavenArtifactVersion(propertyValue, url, Some(response)).failure
    failure.exception shouldBe a[MavenPropertyResolver.MavenError]
    failure.exception.getMessage should include(url)
  }

  it should "resolve the latest stable version when the latest version is not stable" in {
    val propertyValue = "maven(org.scalatest, scalatest_2.12, stable)"
    val url           = "https://repo1.maven.org/maven2/org/scalatest/scalatest_2.12/maven-metadata.xml"
    val response =
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

    resolveMavenArtifactVersion(propertyValue, url, Some(response)).success.value shouldEqual "3.0.1"
  }

  it should "return the latest version if the latest version is stable" in {
    val url           = "https://repo1.maven.org/maven2/org/scalacheck/scalacheck_2.12/maven-metadata.xml"
    val propertyValue = "maven(org.scalacheck, scalacheck_2.12, stable)"
    val response =
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

    resolveMavenArtifactVersion(propertyValue, url, Some(response)).success.value shouldEqual "1.13.5"
  }

  it should "return an error if the latest stable version is not found" in {
    val propertyValue = "maven(org.scalaz, scalaz-concurrent_2.12, stable)"
    val url           = "https://repo1.maven.org/maven2/org/scalaz/scalaz-concurrent_2.12/maven-metadata.xml"
    val response =
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

    val failure = resolveMavenArtifactVersion(propertyValue, url, Some(response)).failure
    failure.exception shouldBe a[MavenPropertyResolver.MavenError]
    failure.exception.getMessage should include(url)
    failure.exception.getMessage should include("latest stable")
  }

  private def resolveMavenArtifactVersion(propertyValue: String,
                                          mavenUrl: String,
                                          responseBody: Option[Elem]): Try[String] = {
    val httpClient: HttpClient = mock[HttpClient]
    val response               = HttpResponse(200, "OK", responseBody.map(_.mkString))

    httpClient.execute _ expects HttpGetRequest(mavenUrl) returning Success(response)

    MavenPropertyResolver(httpClient).resolve(Map("foo" -> propertyValue)).map { resolved =>
      resolved.getOrElse("foo", fail("There is no required property in resolved properties map"))
    }
  }

}
