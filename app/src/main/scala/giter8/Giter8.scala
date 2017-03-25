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
import java.util.logging.{Level, Logger}

import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}

sealed trait Ref
case class Tag(name: String) extends Ref
case class Branch(name: String) extends Ref

case class Config(repo: String,
                  ref: Option[Ref]          = None,
                  forceOverwrite: Boolean   = false,
                  directory: Option[String] = None)

class Exit(val code: Int) extends xsbti.Exit

class Giter8 extends xsbti.AppMain {
  import Giter8._

  Logger.getLogger("giter8.Giter8App").setLevel(Level.SEVERE)

  private val git          = new Git(JGitInteractor)
  private val giter8Engine = Giter8Engine(ApacheHttpClient)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit = new Exit(run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String]): Int = {
    val (parameters, options) = args.partition { s =>
      s.matches("""^--(\S+)=(.+)$""")
    }

    val config = parser.parse(options, Config("")) getOrElse {
      parser.usage
      return 1
    }

    val tempdir = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

    val result = for {
      repository <- GitRepository.fromString(config.repo)
      _          <- git.clone(repository, config.ref, tempdir)
      parameters <- Try(Util.parseArguments(parameters))
      res        <- giter8Engine.applyTemplate(tempdir, config.directory, new File("."), parameters, interactive = true)
    } yield res

    if (tempdir.exists) FileUtils.forceDelete(tempdir)

    result match {
      case Success(s) =>
        println(s)
        0
      case Failure(e) =>
        println(e.getMessage)
        1
    }
  }
}

object Giter8 {
  private val home = Option(System.getProperty("G8_HOME")) match {
    case Some(path) => new File(path)
    case None       => new File(System.getProperty("user.home"), ".g8")
  }

  private val EXAMPLES_INFO =
    """|  --paramname=paramval  Set given parameter value and bypass interaction
       |
       |EXAMPLES
       |
       |Apply a template from github
       |    g8 foundweekends/giter8
       |
       |Apply using the git URL for the same template
       |    g8 git://github.com/foundweekends/giter8.git
       |
       |Apply template from a remote branch
       |    g8 foundweekends/giter8 -b some-branch
       |
       |Apply template from a remote tag
       |    g8 foundweekends/giter8 -t some-tag
       |
       |Apply template from a directory that exists in the repo
       |    g8 foundweekends/giter8 -d some-directory/template
       |
       |Apply template from a local repo
       |    g8 file://path/to/the/repo
       |
       |Apply given name parameter and use defaults for all others.
       |    g8 foundweekends/giter8 --name=template-test""".stripMargin

  val parser = new scopt.OptionParser[Config]("giter8") {
    head("g8", giter8.BuildInfo.version)

    // cmd("search").text("Search for templates on github").action { (_, config) =>
    //   config.copy(search = true)
    // }

    arg[String]("<template>").text("git or file URL, or github user/repo").action { (repo, config) =>
      config.copy(repo = repo)
    }

    opt[String]('b', "branch").text("Resolve a template within a given branch").action { (b, config) =>
      config.copy(ref = Some(Branch(b)))
    }

    opt[String]('t', "tag").text("Resolve a template within a given branch").action { (t, config) =>
      config.copy(ref = Some(Tag(t)))
    }

    opt[String]('d', "directory").text("Resolve a template within a given directory").action { (d, config) =>
      config.copy(directory = Some(d))
    }

    opt[Unit]('f', "force").text("Force overwrite of any existing files in output directory").action { (_, config) =>
      config.copy(forceOverwrite = true)
    }

    version("version").text("Display version number")

    note(EXAMPLES_INFO)
  }

  /** Main-class runner just for testing from sbt*/
  def main(args: Array[String]): Unit = {
    val giter8 = new Giter8
    System.exit(giter8.run(args))
  }
}
