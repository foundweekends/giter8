/*
 * Originally by: Lightbend, Inc.
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

import sbt._
import sbt.Keys._
import bintray.{ BintrayKeys, BintrayPlugin }
import com.typesafe.sbt.JavaVersionCheckPlugin
import JavaVersionCheckPlugin.autoImport._

/**
 * Publish to private bintray repository.
 */
object BintrayPublish extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin && BintrayPlugin && JavaVersionCheckPlugin

  override def buildSettings = Seq(
    BintrayKeys.bintrayOrganization := Some("sbt"),
    BintrayKeys.bintrayReleaseOnPublish := false
  )

  override def projectSettings = Seq(
    BintrayKeys.bintrayRepository := "sbt-plugin-releases",
    BintrayKeys.bintrayPackage := "sbt-giter8",
    pomIncludeRepository := { _ => false },
    javaVersionPrefix in javaVersionCheck := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 11 =>
          Some("1.6")
        case _ =>
          Some("1.8")
      }
    }
  )
}

/**
 * Publish to private bintray repository.
 */
object SonatypePublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin && JavaVersionCheckPlugin

  override def projectSettings = Seq(
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    javaVersionPrefix in javaVersionCheck := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 11 =>
          Some("1.6")
        case _ =>
          Some("1.8")
      }
    }
  )
}

/**
 * For projects that are not published.
 */
object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin && BintrayPublish

  override def projectSettings = Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := ()
  )

}
