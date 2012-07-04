package giter8

case class FileInfo(name: String, hash: String, mode: String)

trait Apply extends GitRepo { self: Giter8 =>
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter,FileOutputStream}
  import scala.util.control.Exception.allCatch

  def setFileMode(f: File, mode: String) = allCatch opt {
    if ((mode(3).toString.toInt & 0x1) > 0) {
      f.setExecutable(true)
    }
  }

  private def use[C <: { def close(): Unit }, T](c: C)(f: C => T): T =
    try { f(c) } finally { c.close() }
}

trait GitRepo { self: Giter8 =>

  import java.io.File
  import org.eclipse.jgit.api._
  import scala.util.control.Exception.{allCatch,catching}
  
  val TMP = new File(System.getProperty("java.io.tmpdir"), java.util.UUID.randomUUID().toString)

  def inspect(repo: String,
               branch: Option[String],
               arguments: Seq[String]) = {
    val tmpl = clone(repo, branch)
    G8Helpers.applyTemplate(tmpl, new File("."), arguments)
  }

  // TODO: exeptions handling
  def clone(repo: String, branch: Option[String]) = {
    val cmd = new CloneCommand()
    for(b <- branch)
      cmd.setBranch(b)
    cmd.setURI(repo)
    cmd.setDirectory(TMP)
    cmd.call()
    TMP.deleteOnExit()
    TMP
  }
}

