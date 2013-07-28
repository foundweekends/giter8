package giter8

trait Apply { self: Giter8 =>
  import java.io.File
  import org.apache.commons.io.FileUtils
  import org.eclipse.jgit.api._
  import scala.util.control.Exception.{allCatch,catching}

  private val tempdir =
    new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

  /** Clean temporary directory used for git cloning */
  def cleanup() {
    if (tempdir.exists)
      FileUtils.forceDelete(tempdir)
  }

  def inspect(repo: String,
              branch: Option[String],
              arguments: Seq[String]): Either[String, String] = {
    val tmpl = clone(repo, branch)
    tmpl.right.flatMap(G8Helpers.applyTemplate(_, new File("."), arguments))
  }

  def ghInspect(user: String,
                proj: String,
                branch: Option[String],
                params: Seq[String]) = {
    try {
        inspect("git://github.com/%s/%s.g8.git".format(user, proj),
                branch,
                params)
    } catch {
      case _: org.eclipse.jgit.api.errors.JGitInternalException =>
        // assume it was an access failure, try with ssh
        // after cleaning the clone directory
        cleanup()
        inspect("git@github.com:%s/%s.g8.git".format(user, proj),
                branch,
                params)
    }
  }

  def fileInspect(repo: String, arguments: Seq[String]): Either[String, String] = {
    val tmpl = copy(repo)
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

    val result = branchName.map { b =>
      if(g.branchList().call().asScala.map(_.getName).contains(b))
        Right(tempdir)
      else
        Left("Branch not found: " + b)
    } getOrElse(Right(tempdir))
    g.getRepository.close()
    result
  }

  /** for file:// repositories with no named branch, just do a file copy */
  def copy(filename: String) = {
    val dir = new File(filename)
    if (!dir.isDirectory)
      Left("Not a readable directory: " + filename)
    else {
      FileUtils.copyDirectory(dir, tempdir)
      Right(tempdir)
    }
  }
}
