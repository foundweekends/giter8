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
    tmpl.right.map(G8Helpers.applyTemplate(_, new File("."), arguments))
  }

  def clone(repo: String, branch: Option[String]) = {
    import org.eclipse.jgit.api.ListBranchCommand.ListMode
    import org.eclipse.jgit.lib._
    import scala.collection.JavaConverters._

    val cmd = Git.cloneRepository()
      .setURI("file://" + repo)
      .setDirectory(TMP)

    val branchName = branch.map("refs/heads/" + _)
    for(b <- branchName)
      cmd.setBranch(b)

    val g = cmd.call()

    TMP.deleteOnExit()

    branchName.map { b =>
      if(g.branchList().call().asScala.map(_.getName).contains(b))
        Right(TMP)
      else
        Left("Branch not found: " + b)
    } getOrElse(Right(TMP))
  }
}

