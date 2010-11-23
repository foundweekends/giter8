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
  
  val gh = :/("github.com") / "api" / "v2" / "json"

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
                |List all available templates.
                |    g8 -l
                |
                |List templates with query. For multiple terms, concatenate with a `+` character
                |    g8 -l android+sbt
                |""".stripMargin
}
