package giter8

class Giter8 extends xsbti.AppMain with Discover with Apply {
  import dispatch._

  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  val RemoteTemplates = """^-(l|-list)$""".r

  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  def run(config: xsbti.AppConfiguration) =
    (config.arguments.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Repo(user, proj))) => inspect("%s/%s.g8".format(user, proj), params)
      case (_, Array(RemoteTemplates(_), query)) => discover(Some(query))
      case (_, Array(RemoteTemplates(_))) => discover(None)
      case _ => Left(usage)
    }) fold ({ error =>
      System.err.println("\n%s\n" format error)
      new Exit(1)
    }, { message =>
      println("\n%s\n" format message)
      new Exit(0)
    })

  class Exit(val code: Int) extends xsbti.Exit

  lazy val gh = wrap_creds(:/("github.com") / "api" / "v2" / "json")

  def check_creds = try {
      val fp = System.getProperty("user.home") + "/.gh"
      val p = new java.util.Properties()
      p.load(new java.io.FileInputStream(fp))
      val (t, u, ps) = (p.getProperty("token"),
                        p.getProperty("username"),
                        p.getProperty("password"))
      if (t != null && ! t.isEmpty) Some(u + "/token", t)
      else Some(u, ps)
    } catch { case _ => None }

  def wrap_creds(r: Request) = {
    check_creds match {
      case Some((u,p)) => (r as_! (u,p)).secure
      case None => r
    }
  }

  def http = new Http {
    override def make_logger = new dispatch.Logger {
      val jdklog = java.util.logging.Logger.getLogger("dispatch")
      def info(msg: String, items: Any*) {
        jdklog.info(msg.format(items: _*))
      }
    }
  }
  def usage = """Usage: g8 [TEMPLATE] [OPTION]...
                |Apply specified template or list available templates.
                |
                |OPTIONS
                |    -l, --list
                |        List current giter8 templates on github.
                |
                |    --paramname=paramvalue
                |        Set given parameter value and bypass interaction.
                |
                |Apply template and interactively fulfill parameters.
                |    g8 n8han/giter8
                |
                |Apply given name parameter and use defaults for all others.
                |    g8 n8han/giter8 --name=template-test
                |
                |List available templates.
                |    g8 --list""".stripMargin
}
