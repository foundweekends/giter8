package giter8

trait Apply { self: Giter8 =>
  import java.io.File
  import scalax.file.Path
  import scalax.file.ImplicitConversions.defaultPath2jfile
  import org.eclipse.jgit.api._
  import scala.util.control.Exception.{allCatch,catching}

  val tempdir = Path.createTempDirectory(deleteOnExit = true)

  def inspect(repo: String,
              branch: Option[String],
              arguments: Seq[String]): Either[String, String] = {
    val tmpl = clone(repo, branch)
    tmpl.right.flatMap(G8Helpers.applyTemplate(_, new File("."), arguments))
  }

  def clone(repo: String, branch: Option[String]) = {
    import org.eclipse.jgit.api.ListBranchCommand.ListMode
    import org.eclipse.jgit.lib._
    import scala.collection.JavaConverters._

    val cmd = Git.cloneRepository()
      .setURI(repo)
      .setDirectory(tempdir)

    val branchName = branch.map("refs/heads/" + _)
    for(b <- branchName)
      cmd.setBranch(b)

    val g = cmd.call()

    branchName.map { b =>
      if(g.branchList().call().asScala.map(_.getName).contains(b))
        Right(tempdir)
      else
        Left("Branch not found: " + b)
    } getOrElse(Right(tempdir))
  }
}
