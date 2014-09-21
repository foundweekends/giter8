package giter8

class Giter8 extends xsbti.AppMain with Apply {
  import dispatch._

  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit =
    new Exit(Giter8.run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String]): Int = {
    val result = (args.partition { s =>
      G8Helpers.Param.pattern.matcher(s).matches
    } match {
      case (params, options) =>
        parser.parse(options, Config()).map { config =>
          if (config.search)
            search(config)
          else
            inspect(config, params)
        }.getOrElse(Left(""))
      case _ => Left(parser.usage)
    })
    cleanup()
    result.fold ({ (error: String) =>
      System.err.println("\n%s\n" format error)
      1
    }, { (message: String) =>
      println("\n%s\n" format message )
      0
    })
  }

  val parser = new scopt.OptionParser[Config]("giter8") {
    head("g8", giter8.BuildInfo.version)
    cmd("search") action { (_, config) =>
      config.copy(search = true)
    } text("Search for templates on github")
    arg[String]("<template>") action { (repo, config) =>
      config.copy(repo = repo)
    } text ("git or file URL, or github user/repo")
    opt[String]('b', "branch") action { (b, config) => 
      config.copy(branch = Some(b))
    } text("Resolve a template within a given branch")
    opt[String]('t', "tag") action { (t, config) =>
      if (config.branch.nonEmpty) {
        System.err.println("\nDo not specify branch and tag in the meantime\n")
        System.exit(1)
      }
      config.copy(tag = Some(t))
    } text("Resolve a template within a given tag")
    opt[Unit]('f', "force") action { (_, config) =>
      config.copy(forceOverwrite = true)
    } text("Force overwrite of any existing files in output directory")
    note("""  --paramname=paramvalue
      |        Set given parameter value and bypass interaction.
      |
      |EXAMPLES
      |
      |Apply a template from github
      |    g8 n8han/giter8
      |
      |Apply using the git URL for the same template
      |    g8 git://github.com/n8han/giter8.git
      |
      |Apply template from a remote branch
      |    g8 n8han/giter8 -b some-branch
      |
      |Apply template from a remote tag
      |    g8 n8han/giter8 -t some-tag
      |
      |Apply template from a local repo
      |    g8 file://path/to/the/repo
      |
      |Apply given name parameter and use defaults for all others.
      |    g8 n8han/giter8 --name=template-test""".stripMargin)
  }
}

class Exit(val code: Int) extends xsbti.Exit

object Giter8 extends Giter8 {
  import java.io.File
  val home = Option(System.getProperty("G8_HOME")).map(new File(_)).getOrElse(
    new File(System.getProperty("user.home"), ".g8")
  )

  /** Main-class runner just for testing from sbt*/
  def main(args: Array[String]) {
    System.exit(run(args))
  }
}
