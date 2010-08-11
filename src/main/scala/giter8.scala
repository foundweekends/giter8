package giter8

class Giter8 extends xsbti.AppMain {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._

  val re = "^src/main/g8/(.+)$".r
  
  def run(config: xsbti.AppConfiguration) = {
    for {
      repo <- config.arguments.headOption map { _ + ".g8" }
      blobs <- http(gh / "blob" / "all" / repo / "master" ># ('blobs ? obj))
      JField(name, JString(value)) <- blobs
      m <- re.findFirstMatchIn(name)
    } {
      import java.io.{File,FileOutputStream}
      val f = new File(m.group(1))
      new File(f.getParent).mkdirs()
      http(gh / "blob" / "show" / repo / value >>> new FileOutputStream(f))
    }
    new Exit(1)
  }
  class Exit(val code: Int) extends xsbti.Exit
  def http = new Http
  val gh = :/("github.com") / "api" / "v2" / "json"
}

