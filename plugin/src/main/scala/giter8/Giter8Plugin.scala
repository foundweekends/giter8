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

import sbt.ScriptedPlugin._
import sbt._

import scala.util.{Failure, Success}

object Giter8Plugin extends sbt.AutoPlugin {
  override val requires = sbt.plugins.JvmPlugin
  override val trigger  = allRequirements

  import Keys._

  object autoImport {
    lazy val g8              = taskKey[Unit]("Apply default parameters to input templates and write to output.")
    lazy val g8PropertyFiles = settingKey[Seq[File]]("g8-properties-file")
    lazy val g8Template      = settingKey[Template]("Giter8 template")
    lazy val g8Properties    = taskKey[Map[String, String]]("g8-properties")
    lazy val g8TestScript    = settingKey[File]("g8-test-script")
    lazy val g8Test          = inputKey[Unit]("Run `sbt test` in output to smoke-test the templates")
  }

  import autoImport._

  lazy val baseGiter8Settings: Seq[Def.Setting[_]] = Seq(
    g8 := {
      val base     = (unmanagedSourceDirectories in g8).value
      val srcs     = (sources in g8).value
      val out      = (target in g8).value
      val props    = (g8Properties in g8).value
      val template = (g8Template in g8).value
      IO.delete(out)
      //Giter8(srcs pair relativeTo(base), out, props)

      (for {
        packageDir <- Success(props.get("name").map(FormatFunctions.normalize).getOrElse("."))
        res        <- TemplateRenderer.render(template.root, template.templateFiles, out / packageDir, props)
        _          <- TemplateRenderer.copyScaffolds(template.scaffoldsRoot, template.scaffoldsFiles, out / ".g8")
      } yield res) match {
        case Success(s) => println("Template applied")
        case Failure(e) => println(e.getMessage)
      }
    },
    aggregate in g8 := false,
    unmanagedSourceDirectories in g8 := {
      val g8directory = (sourceDirectory.value / "g8").get
      if (g8directory.nonEmpty) g8directory
      else Seq(baseDirectory.value)
    },
    sources in g8 := {
      (g8Template in g8).value.templateFiles
    },
    g8Template in g8 := {
      val dirs = (unmanagedSourceDirectories in g8).value
      Template(dirs.head)
    },
    target in g8 := target.value / "g8",
    g8PropertyFiles in g8 := (g8Template in g8).value.propertyFiles,
    g8Properties in g8 := {
      val resolver = PropertyResolverChain(
        FilePropertyResolver((g8PropertyFiles in g8).value: _*),
        MavenPropertyResolver(Giter8.defaultHttpClient)
      )
      resolver.resolve(Map.empty).getOrElse(Map.empty)
    }
  )

  lazy val giter8TestSettings: Seq[Def.Setting[_]] = scriptedSettings ++ Seq(
      g8Test in Test := { scriptedTask.evaluated },
      aggregate in (Test, g8Test) := false,
      scriptedDependencies := {
      val x = (g8 in Test).value
    },
      g8 in Test := {
      val base     = (unmanagedSourceDirectories in (Compile, g8)).value
      val srcs     = (sources in (Compile, g8)).value
      val out      = (target in (Test, g8)).value
      val props    = (g8Properties in (Test, g8)).value
      val ts       = (g8TestScript in (Test, g8)).value
      val s        = streams.value
      val template = (g8Template in (Test, g8)).value
      IO.delete(out)
//      val retval = Giter8(srcs pair relativeTo(base), out, props)
      (for {
        packageDir <- Success(props.get("name").map(FormatFunctions.normalize).getOrElse("."))
        res        <- TemplateRenderer.render(template.root, template.templateFiles, out / packageDir, props)
        _          <- TemplateRenderer.copyScaffolds(template.scaffoldsRoot, template.scaffoldsFiles, out / ".g8")
      } yield res) match {
        case Success(s) => println("Template applied")
        case Failure(e) => println(e.getMessage)
      }

      // copy test script or generate one
      val script = new File(out, "test")
      if (ts.exists) IO.copyFile(ts, script)
      else IO.write(script, """>test""")
//      retval :+ script
    },
      sbtTestDirectory := { target.value / "sbt-test" },
      target in (Test, g8) := { sbtTestDirectory.value / name.value / "scripted" },
      g8TestScript := {
      val dir     = (sourceDirectory in Test).value
      val metadir = (baseDirectory in LocalRootProject).value / "project"
      val file0   = dir / "g8" / "test"
      val files = List(file0,
                       dir / "g8" / "giter8.test",
                       dir / "g8" / "g8.test",
                       metadir / "test",
                       metadir / "giter8.test",
                       metadir / "g8.test")
      files.find(_.exists).getOrElse(file0)
    },
      scriptedBufferLog in (Test, g8) := true
    )

  override lazy val projectSettings: Seq[Def.Setting[_]] = inConfig(Compile)(baseGiter8Settings) ++ giter8TestSettings
}
