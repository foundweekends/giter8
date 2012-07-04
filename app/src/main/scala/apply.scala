package giter8

trait Apply { self: Giter8 =>
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

