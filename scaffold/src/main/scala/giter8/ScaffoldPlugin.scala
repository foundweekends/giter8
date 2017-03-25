/*
 * Original implementation (C) 2012-2016 Julien Tournay and contributors
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

import sbt.Keys._
import sbt._

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

object ScaffoldPlugin extends sbt.AutoPlugin {
  override val requires = sbt.plugins.JvmPlugin
  override val trigger  = allRequirements

  object autoImport {
    lazy val g8ScaffoldTemplatesDirectory = settingKey[File]("Directory used to hold scaffolding templates.")
    lazy val g8Scaffold                   = inputKey[Unit]("Runs scaffolding")
  }

  import autoImport._
  import complete.DefaultParsers._
  import complete._

  private val parser = Def.setting { state: State =>
    val dir                 = g8ScaffoldTemplatesDirectory.value
    val templateDirectories = Option(dir.listFiles).toList.flatten.filter(f => f.isDirectory && !f.isHidden)

    val templateParsers = templateDirectories.map(_.getName: Parser[String])
    val nameParser      = token(templateParsers.reduce(_ | _)).examples("<template>")
    val argumentParser  = Space ~> StringBasic.examples("--k=v")

    Space ~> nameParser ~ argumentParser.* map {
      case tmp ~ args => (tmp, args)
    }
  }

  private val scaffoldTask = Def.inputTask {
    val (name, args) = parser.parsed
    val folder       = g8ScaffoldTemplatesDirectory.value
    val log          = streams.value.log
    val giter8Engine = Giter8Engine(ApacheHttpClient)
    giter8Engine.applyTemplate(folder / name, None, baseDirectory.value, Util.parseArguments(args), interactive = true) match {
      case Success(s) => log.info(s"Template '$name' applied")
      case Failure(e) => log.error(e.getMessage)
    }
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    g8ScaffoldTemplatesDirectory := baseDirectory.value / ".g8",
    g8Scaffold := scaffoldTask.evaluated
  )
}
