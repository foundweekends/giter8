package giter8

class Giter8 extends xsbti.AppMain with Discover with Apply {
  import dispatch._

  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  val RemoteTemplates = """^-l(ist)?$""".r
  
  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)
  
  def run(config: xsbti.AppConfiguration) =
    (config.arguments.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Repo(user, proj))) => inspect("%s/%s.g8".format(user, proj), params)
      case (_, Array(RemoteTemplates(_), query)) => discover(Some(query))
      case (_, Array(RemoteTemplates(_))) => discover(None)
      case _ => Left("Usage: g8 <gituser/project.g8> [--param=value ...]")
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
}
