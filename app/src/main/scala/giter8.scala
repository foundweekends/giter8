package giter8

class Giter8 extends xsbti.AppMain
  with Discover with Apply with Authorize with Credentials {
  import dispatch._

  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  val Branch = """^-(b|-branch)$""".r
  val RemoteTemplates = """^-(l|-list)$""".r
  val Auth = """^-(a|-auth)$""".r
  val Git = """^(.*\.g8\.git)$""".r

  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Exit =
    new Exit(Giter8.run(config.arguments))

  /** Runner shared my main-class runner */
  def run(args: Array[String]): Int = {
    (args.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Git(remote))) => 
        GitRepo.inspect(remote, None, params)
      case (params, Array(Git(remote), Branch(_), branch)) =>
        GitRepo.inspect(remote, Some(branch), params)
      case (params, Array(Repo(user, proj))) =>
        inspect("%s/%s.g8".format(user, proj), None, params)
      case (params, Array(Repo(user, proj), Branch(_), branch)) =>
        inspect("%s/%s.g8".format(user, proj), Some(branch), params)
      case (params, Array(Auth(param), userpass)) =>
        userpass.split(":", 2) match {
          case Array(user, pass) => auth(user, pass)
          case _ =>
            Left("-%s requires username and password separated by `:`".format(
              param))
        }
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
                |    -a, --auth <login>:<password>
                |        Authorizes oauth access to Github
                |    -b, --branch
                |        Resolves a template within a given branch
                |    --paramname=paramvalue
                |        Set given parameter value and bypass interaction.
                |    
                |
                |Apply template and interactively fulfill parameters.
                |    g8 n8han/giter8
                |
                |Apply template from a remote branch
                |    g8 n8han/giter8 -b some-branch
                |
                |Apply given name parameter and use defaults for all others.
                |    g8 n8han/giter8 --name=template-test
                |
                |Acquire Github authorization
                |    g8 -a login:password
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
