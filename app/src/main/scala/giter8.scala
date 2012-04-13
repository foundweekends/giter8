package giter8

class Giter8 extends xsbti.AppMain with Discover with Apply with Credentials {
  import dispatch._

  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  val Branch = """^-(b|-branch)$""".r
  val RemoteTemplates = """^-(l|-list)$""".r

  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit =
    new Exit(Giter8.run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String]): Int = {
    (args.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Repo(user, proj))) =>
        inspect("%s/%s.g8".format(user, proj), None, params)
      case (params, Array(Repo(user, proj), Branch(_), branch)) =>
        inspect("%s/%s.g8".format(user, proj), Some(branch), params)
      case _ => Left(usage)
    }) fold ({ error =>
      System.err.println("\n%s\n" format error)
      1
    }, { message =>
      println("\n%s\n" format message)
      0
    })
  }

  def gh = withCredentials(:/("api.github.com").secure) / "repos"

  def http = new Http {
    override def make_logger = new dispatch.Logger {
      val jdklog = java.util.logging.Logger.getLogger("dispatch")
      def info(msg: String, items: Any*) {
        jdklog.info(msg.format(items: _*))
      }
      def warn(msg: String, items: Any*) {
        jdklog.warning(msg.format(items: _*))
      }
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
                |Apply template and interactively fulfill parameters.
                |    g8 n8han/giter8
                |
                |Apply template from a remote branch
                |    g8 n8han/giter8 -b some-branch
                |
                |Apply given name parameter and use defaults for all others.
                |    g8 n8han/giter8 --name=template-test
                |""".stripMargin format (BuildInfo.version)

}

class Exit(val code: Int) extends xsbti.Exit

object Giter8 extends Giter8 {
  /** Main-class runner just for testing from sbt*/
  def main(args: Array[String]) {
    System.exit(run(args))
  }
}
