package giter8

import java.io.{File, FileInputStream}
import java.util.Properties

import org.apache.commons.io.FileUtils

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Launcher {

  import giter8.G8.RichFile

  private val interactor = new Git(new JGitInteractor())

  private def tempdir = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

  /**
    * Runs a block in passing a `File` of a temporary dir to the function
    * cleans up the tempdir once the block runs
    *
    * @param block
    * @tparam A
    * @return
    */
  private def withTempdir[A](block: File => A): Try[A] = {

    val file   = tempdir
    val result = Try(block(file))

    if (file.exists) FileUtils.forceDelete(file)

    result
  }

  /**
    * Clones a repo locally and executes the block, passing a `File` to the base
    * of the cloned repo.
    *
    * Uses a tempdir which is cleaned up after the block runs
    *
    * @param repo
    * @param tag
    * @param block
    * @tparam A
    * @return
    */
  private def withRepo[A](repo: String, tag: Option[String])(block: File => A): Try[A] = withTempdir {
    file =>

      // TODO branch support?
      val ref = tag.map(v => Tag(s"v$v"))

      for {
        repository <- GitRepository.fromString(repo) match {
          case Right(r) => Success(r)
          case Left(msg) => Failure(new RuntimeException(msg))
        }
        _          <- interactor.clone(repository, ref, file)
      } yield block(file)
  }.flatten

  private def withRepo[A](repo: String)(block: File => A): Try[A] =
    withRepo(repo, None)(block)

  /**
    * Filters the input arguments to remove `--g8Version` and its
    * value so that the fetched instance of giter8 won't continually
    * try to retrieve itself.
    *
    * @param args
    * @return
    */
  private def filteredArgs(args: Array[String]): Array[String] = {

    val i = args.indexOf("--g8Version")

    if (i > -1) {
      args.zipWithIndex.foldLeft[List[String]](Nil) {
        case (m, (arg, j)) if j == i || j == (i + 1) =>
          m
        case (m, (arg, _)) =>
          arg :: m
      }.toArray
    } else {
      args
    }
  }

  /**
    * Currently we use Conscript's version of the `sbt-launch.jar` to
    * launch the new instance of giter8.
    */
  private lazy val conscriptHome: Try[String] = sys.env.get("CONSCRIPT_HOME")
    .map(Success.apply)
    .getOrElse {
      Failure(new RuntimeException("Conscript must be installed - WIP"))
    }

  /**
    * Use JAVA_HOME as a default location for finding a java executable.
    */
  private lazy val javaHome: Try[Option[String]] = Success(sys.env.get("JAVA_HOME"))

  private implicit class OptTry[A](a: Try[Option[A]]) {

    def or(b: Try[Option[A]]): Try[Option[A]] =
      a.flatMap {
        unwrapped =>
          if (unwrapped.isDefined) a else b
      }
  }

  /**
    * Resolves the version of giter8 to be launched.
    *
    * If `g8Version` is defined, that will be used. Otherwise, we clone
    * the template repo and look for a `giter8.version` property in `project/build.properties`
    *
    * Returns `None` if we can't find a version from either of these
    *
    * @param g8Version
    * @param template
    * @return
    */
  private def version(g8Version: Option[String], template: String): Try[Option[String]] =
    Success(g8Version) or withRepo(template) {
      base =>
        Try {
          val properties: Properties = {
            val p = new Properties()
            p.load(new FileInputStream(base / "project" / "build.properties"))
            p
          }
          val result: Option[String] = Option(properties.getProperty("giter8.version"))
          result
        }
    }.flatten

  /**
    * Retrieves the launch config of a specific tagged version of Giter8,
    * if `version` is undefined, we attempt to retrieve the latest version
    *
    * NOTE: It seems that the version number in some of the launchconfigs
    * of does not match the release version.
    *
    * e.g. https://github.com/foundweekends/giter8/blob/v0.9.0/src/main/conscript/g8/launchconfig#L2
    *
    * @param version
    * @return
    */
  private def launchConfig(version: Option[String]): Try[String] =
    withRepo("https://github.com/foundweekends/giter8", version) {
      base =>
        val lcFile = base / "src" / "main" / "conscript" / "g8" / "launchconfig"
        val result: String = Source.fromFile(lcFile).getLines.mkString("\r\n")
        println(s"LaunchConfig resolved for version: ${version.getOrElse("LATEST")}")
        result
    }

  /**
    * Creates another JVM process running Conscript's `sbt-launch.jar` with the resolved
    * `launchconfig`
    *
    * This downloads the version of giter8 for that launchconfig and passes on
    * the arguments that this process was created with (minus some filtered args)
    *
    * @param csHome
    * @param lc
    * @param args
    * @param v
    * @param jHome
    * @return
    */
  private def fetchAndRun(
                           csHome: String,
                           lc: String,
                           args: Array[String],
                           v: Option[String],
                           jHome: Option[String] = None
                         ): Try[String] =
    withTempdir {
      base =>

        import scala.sys.process._

        val lcFile: File = base / "launchconfig"
        FileUtils.write(lcFile, lc)

        /**
          *  Attempt to use JAVA_HOME to find a java executable, if
          *  no JAVA_HOME is defined, fall back to PATH
          */
        val java = jHome.map(j => s"$j/bin/java").getOrElse("java")

        println(s"Fetching Giter8 ${v.getOrElse("LATEST")}")

        // TODO add "-r" flag for versions >= when this is released, as otherwise we may end up in a loop
        val command = Seq(java, "-jar", s"$csHome/sbt-launch.jar", s"@$lcFile" /*, "-r" */) ++ filteredArgs(args)
        //        val command = Seq(java, "-jar", s"$csHome/sbt-launch.jar", s"@$lcFile", "-r") ++ filteredArgs(args)

        val exit = command.run(BasicIO.standard(true)).exitValue()

        if (exit == 0) {
          Success("Success!")
        } else {
          Failure(new RuntimeException(s"Failure, exit code: $exit"))
        }
    }.flatten

  def launch(template: String, g8Version: Option[String], args: Array[String]): Try[String] =
    for {
      jHome  <- javaHome
      csHome <- conscriptHome
      v      <- version(g8Version, template)
      lc     <- launchConfig(v)
      result <- fetchAndRun(csHome, lc, args, v, jHome)
    } yield result
}

