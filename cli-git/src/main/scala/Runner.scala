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

import scopt.OptionParser
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success}

class Runner {
  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  val Param = """(?s)^--(\S+)=(.+)$""".r

  /**
    * Parses command line argument, clones the template, and runs the processor.
    */
  def run(args: Array[String], workingDirectory: File, processor: Processor): Int = {
    def clone(repo: GitRepository, ref: Option[Ref], tempdir: File, knownHosts: Option[String]): Either[String, File] =
      new Git(new JGitInteractor(knownHosts)).clone(repo, ref, tempdir) match {
        case Success(_) => Right(tempdir)
        case Failure(e) => Left(e.getMessage)
      }

    val result: Either[String, String] = (args.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, options) =>
        val tempdir = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)
        try {
          for {
            config   <- parser.parse(options, Config("")).map(Right(_)).getOrElse(Left(""))
            repo     <- GitRepository.fromString(config.repo)
            cloneDir <- clone(repo, config.ref, tempdir, config.knownHosts)
            templateDir     = new File(cloneDir, config.directory.getOrElse(""))
            outputDirectory = config.out.map(new File(_))
            p <- processor.process(templateDir, workingDirectory, params, config.forceOverwrite, outputDirectory)
          } yield p
        } finally {
          if (tempdir.exists) FileUtils.forceDelete(tempdir)
        }
      case _ => Left(parser.usage)
    })
    result match {
      case Left(error) =>
        if (error != "") {
          Console.err.println(s"\n$error\n")
        }
        1
      case Right(message) =>
        if (message != "") {
          Console.out.println(s"\n$message\n")
        }
        0
    }
  }

  def run(args: Array[String], processor: Processor): Int = run(args, new File(".").getAbsoluteFile, processor)

  val parser: OptionParser[Config] = new scopt.OptionParser[Config]("g8") {

    head("g8", giter8.BuildInfo.version)

    arg[String]("<template>") action { (repo, config) =>
      config.copy(repo = repo)
    } text "git or file URL, or GitHub user/repo"

    opt[String]('b', "branch") action { (b, config) =>
      config.copy(ref = Some(Ref.Branch(b)))
    } text "Resolve a template within a given branch"

    opt[String]('t', "tag") action { (t, config) =>
      config.copy(ref = Some(Ref.Tag(t)))
    } text "Resolve a template within a given tag"

    opt[String]('d', "directory") action { (d, config) =>
      config.copy(directory = Some(d))
    } text "Resolve a template within the given subdirectory in the repo"

    opt[String]('o', "out") action { (o, config) =>
      config.copy(out = Some(o))
    } text "Output directory"

    opt[Unit]('f', "force") action { (_, config) =>
      config.copy(forceOverwrite = true)
    } text "Force overwrite of any existing files in output directory"

    opt[String]('h', "known-hosts") action { (h, config) =>
      config.copy(knownHosts = Some(h))
    } text "SSH known hosts file. If unset the location will be guessed."

    version("version").text("Display version number")

    note("""  --paramname=paramval  Set given parameter value and bypass interaction
      |
      |EXAMPLES
      |
      |Apply a template from GitHub
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
      |Apply template into an output directory
      |    g8 foundweekends/giter8 -o output-directory
      |
      |Apply template from a local repo
      |    g8 file://path/to/the/repo
      |
      |Apply given name parameter and use defaults for all others.
      |    g8 foundweekends/giter8 --name=template-test""".stripMargin)
  }
}
