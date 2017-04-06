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

import scala.util.{Failure, Success, Try}

object Giter8Plugin extends sbt.AutoPlugin {
  override val requires = sbt.plugins.JvmPlugin
  override val trigger  = allRequirements

  import Keys._

  object autoImport {
    lazy val g8                  = taskKey[Unit]("Apply default parameters to input templates and write to output")
    lazy val g8TemplateDirectory = settingKey[File]("Giter8 template directory")
    lazy val g8Template          = settingKey[Template]("Giter8 template")
    lazy val g8TemplateFiles     = settingKey[Seq[File]]("Giter8 template files")
    lazy val g8PropertyFiles     = settingKey[Seq[File]]("Giter8 template property files")
    lazy val g8Properties        = taskKey[Map[String, String]]("Giter8 template default properties")
    lazy val g8TargetDirectory   = settingKey[File]("Directory to copy rendered template")
    lazy val g8TestScript        = settingKey[File]("Giter8 test script")
    lazy val g8Test              = inputKey[Unit]("Run `sbt test` in output to smoke-test the templates")
  }

  private lazy val copyG8TestScript = taskKey[Unit]("Copy Giter8 sbt-test script")

  import autoImport._

  lazy val baseGiter8Settings: Seq[Def.Setting[_]] = Seq(
    aggregate in g8 := false,
    g8TargetDirectory := target.value / "g8",
    g8TemplateDirectory := {
      Option(sourceDirectory.value / "g8") match {
        case Some(directory) if directory.isDirectory && directory.listFiles.nonEmpty => directory
        case _                                                                        => baseDirectory.value
      }
    },
    g8Template := Template(g8TemplateDirectory.value),
    g8TemplateFiles := g8Template.value.templateFiles,
    g8PropertyFiles := g8Template.value.propertyFiles,
    g8Properties := {
      val resolver = PropertyResolverChain(
        FilePropertyResolver((g8PropertyFiles in g8).value: _*),
        MavenPropertyResolver(ApacheHttpClient)
      )
      resolver.resolve(Map.empty).getOrElse(Map.empty)
    },
    g8 := {
      val out      = g8TargetDirectory.value
      val props    = g8Properties.value
      val template = g8Template.value

      applyTemplate(out, props, template) match {
        case Success(s) => println("Template applied")
        case Failure(e) =>
          println(e.getMessage)
          sys.exit(1)
      }
    }
  )

  lazy val giter8TestSettings: Seq[Def.Setting[_]] = Seq(
    aggregate in g8Test := false,
    sbtTestDirectory in g8Test := target.value / "sbt-test",
    scriptedBufferLog in g8Test := true,
    g8TargetDirectory in g8Test := sbtTestDirectory.value / name.value / "scripted",
    scriptedDependencies := {
      g8.value
      copyG8TestScript.value
    },
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
    copyG8TestScript := {
      val testScript = g8TestScript.value
      val out        = g8TargetDirectory.value

      // copy test script or generate one
      val script = new File(out, "test")
      if (testScript.exists) IO.copyFile(testScript, script)
      else IO.write(script, """> test""")
    },
    g8Test := scriptedTask.evaluated
  )

  private def applyTemplate(out: File, props: Map[String, String], template: Template): Try[Unit] = {
    IO.delete(out)
    for {
      packageDir <- Success(props.get("name").map(FormatFunctions.normalize).getOrElse("."))
      res        <- TemplateRenderer.render(template.root, template.templateFiles, out / packageDir, props, force = true)
      _          <- TemplateRenderer.copyScaffolds(template.scaffoldsRoot, template.scaffoldsFiles, out / ".g8")
    } yield res
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] = baseGiter8Settings ++ scriptedSettings ++ inConfig(Test)(
    giter8TestSettings)
}
