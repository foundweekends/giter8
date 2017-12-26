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

import giter8.launcher.Launcher
import scopt.OptionParser

import scala.util.{Failure, Success}

class Giter8 extends xsbti.AppMain {
  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit =
    new Exit(Giter8.run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String], baseDirectory: File): Int = {
    val result = args.partition { s =>
      G8.Param.pattern.matcher(s).matches
    } match {
      case (params, options) =>
        parser
          .parse(options, Config(""))
          .map {
            case config if config.mode == Launch && !isForked =>
              Launcher.launch(config.repo, config.version, args) match {
                case Success(msg) => Right(msg)
                case Failure(e)   => Left(e.getMessage)
              }
            case config =>
              val helper = new JgitHelper(new Git(new JGitInteractor), G8TemplateRenderer)
              val result = helper.run(config, params, baseDirectory)
              helper.cleanup()
              result
          }
          .getOrElse(Left(""))
      case _ => Left(parser.usage)
    }
    result.fold({ (error: String) =>
      System.err.println(s"\n$error\n")
      1
    }, { (message: String) =>
      println("\n%s\n" format message)
      0
    })
  }

  def run(args: Array[String]): Int = run(args, new File(".").getAbsoluteFile)

  val parser: OptionParser[Config] = new scopt.OptionParser[Config]("g8") {

    head("g8", giter8.BuildInfo.version)

    arg[String]("<template>") action { (repo, config) =>
      config.copy(repo = repo)
    } text "git or file URL, or github user/repo"

    opt[String]('b', "branch") action { (b, config) =>
      config.copy(ref = Some(Branch(b)))
    } text "Resolve a template within a given branch"

    opt[String]('t', "tag") action { (t, config) =>
      config.copy(ref = Some(Tag(t)))
    } text "Resolve a template within a given tag"

    opt[String]('d', "directory") action { (d, config) =>
      config.copy(directory = Some(d))
    } text "Resolve a template within a given directory"

    opt[String]('o', "out") action { (o, config) =>
      config.copy(out = Some(o))
    }

    opt[Unit]('f', "force") action { (_, config) =>
      config.copy(forceOverwrite = true)
    } text "Force overwrite of any existing files in output directory"

    opt[Unit]('r', "run") action { (_, config) =>
      config.copy(mode = Run)
    }

    opt[String]("g8Version") action { (version, config) =>
      config.copy(mode = Launch, version = Some(version))
    }

    version("version").text("Display version number")

    note("""  --paramname=paramval  Set given parameter value and bypass interaction
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
      |    g8 foundweekends/giter8 --name=template-test""".stripMargin)
  }

  /**
    * Checks a system property which the launcher sets to `true` when
    * it launches the new Giter8 instance, this will stop the launcher
    * from recursively starting new instances forever.
    *
    * This is a system property instead of command argument so that
    * previous versions of giter8 won't complain about an unknown
    * parameter.
    *
    * @return
    */
  private def isForked: Boolean =
    sys.env.get("GITER8_FORKED").exists(_ == "true")
}

class Exit(val code: Int) extends xsbti.Exit

object Giter8 extends Giter8 {
  import java.io.File
  val home = Option(System.getProperty("G8_HOME"))
    .map(new File(_))
    .getOrElse(
      new File(System.getProperty("user.home"), ".g8")
    )

  /** Main-class runner just for testing from sbt*/
  def main(args: Array[String]): Unit = {
    System.exit(run(args))
  }
}
