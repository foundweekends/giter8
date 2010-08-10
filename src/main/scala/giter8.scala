package giter8

class Giter8 extends xsbti.AppMain {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._

  def run(config: xsbti.AppConfiguration) = {
    for {
      repo <- config.arguments.headOption map { _ + ".g8" }
      blob <- http(gh / "blob" / "all" / repo / "master" ># ('blobs ? obj))
    } {
      val JField(name, JString(value)) = blob
      println(http(gh / "blob" / "show" / repo / value as_str))
    }
    new Exit(1)
  }
  class Exit(val code: Int) extends xsbti.Exit
  def http = new Http
  val gh = :/("github.com") / "api" / "v2" / "json"
}

