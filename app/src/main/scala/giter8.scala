package giter8

class Giter8 extends xsbti.AppMain with Apply {
  import dispatch._

  import G8Helpers.Regs._

  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit =
    new Exit(Giter8.run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String]): Int = {
    (args.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Local(repo))) => 
        inspect(repo, None, params)
      case (params, Array(Local(repo), Branch(_), branch)) => 
        inspect(repo, Some(branch), params)
      case (params, Array(Git(remote))) => 
        inspect(remote, None, params)
      case (params, Array(Git(remote), Branch(_), branch)) =>
        inspect(remote, Some(branch), params)
      case (params, Array(Repo(user, proj))) =>
        ghInspect(user, proj, None, params)
      case (params, Array(Repo(user, proj), Branch(_), branch)) =>
        ghInspect(user, proj, Some(branch), params)
      case _ => Left(usage)
    }) fold ({ error =>
      System.err.println("\n%s\n" format error)
      1
    }, { message =>
      println("\n%s\n" format message)
      0
    })
  }

  def ghInspect(user: String,
                proj: String,
                branch: Option[String],
                params: Seq[String]) = {
    try {
        inspect("git://github.com/%s/%s.g8.git".format(user, proj),
                branch,
                params)
    } catch {
      case _: org.eclipse.jgit.api.errors.JGitInternalException =>
        inspect("git@github.com:%s/%s.g8.git".format(user, proj),
                branch,
                params)
    }
  }

  def usage = """giter8 %s
                |Usage: g8 [TEMPLATE] [OPTION]...
                |Apply specified template.
                |
                |OPTIONS
                |    -b, --branch
                |        Resolves a template within a given branch
                |    --paramname=paramvalue
                |        Set given parameter value and bypass interaction.
                |
                |
                |Apply template and interactively fulfill parameters.
                |    g8 n8han/giter8
                |
                |Or
                |    g8 git://github.com/n8han/giter8.git
                |
                |Apply template from a remote branch
                |    g8 n8han/giter8 -b some-branch
                |
                |Apply template from a local repo
                |    g8 file://path/to/the/repo
                |
                |Apply given name parameter and use defaults for all others.
                |    g8 n8han/giter8 --name=template-test
                |
                |""".stripMargin format (BuildInfo.version)

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
