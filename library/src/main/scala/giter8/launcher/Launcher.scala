package giter8.launcher

import java.io.{File, FileInputStream}
import java.util.Properties

import giter8.G8.RichFile
import giter8.{Git, JGitInteractor, SystemPaths, TempDir, VersionNumber}

import scala.util.{Failure, Success, Try}

object Launcher extends TempDir {

  private lazy val git = new Git(new JGitInteractor())

  /**
    * Looks up the `giter8.version` property from the template
    *
    * @param template
    * @return
    */
  private def templateVersion(template: String): Try[Option[VersionNumber]] =
    git.withRepo(template) {
      base =>
        Try {
          val properties: Properties = {
            val p = new Properties()
            p.load(new FileInputStream(base / "project" / "build.properties"))
            p
          }
          val result: Option[String] = Option(properties.getProperty("giter8.version"))
          result
        }.flatMap {
          string =>
            Try(string.map(VersionNumber.apply))
        }
    }.flatten

  /**
    * Resolves the version from `giter8.version` system property
    */
  private lazy val propertyVersion: Try[Option[VersionNumber]] =
    Success(sys.env.get("giter8.version").map(VersionNumber.apply))

  /**
    * Resolves the version of giter8 to be launched.
    *
    * Precedence:
    *   `--g8Version` command line argument
    *   Looks up the `giter8.version` property from the template project
    *   `giter8.version` system property
    *
    * Returns `None` if we can't find a version from either of these
    *
    * @param g8Version
    * @param template
    * @return
    */
  private def version(g8Version: Option[String], template: String): Try[Option[VersionNumber]] = {
    Try(g8Version.map(VersionNumber.apply)) or
      templateVersion(template) or
      propertyVersion
  }

  /**
    * Creates another JVM process running the `sbt-launcher` jar with the resolved
    * `launchconfig`
    *
    * This downloads the version of giter8 for that launchconfig and passes on
    * the arguments that this process was created with (minus some filtered args)
    *
    * @param launchJar
    * @param lc
    * @param args
    * @param v
    * @return
    */
  private def fetchAndRun(
                           launchJar: File,
                           lc: File,
                           args: Array[String],
                           v: Option[VersionNumber]
                         ): Try[String] =
    withTempdir {
      base =>

        import scala.sys.process._

        val java: File =
          SystemPaths.javaHome / "bin" / "java"

        println(s"Fetching Giter8 ${v.getOrElse("LATEST")}")

        val command = Seq(
          java.getPath, "-DGITER8_FORKED=true", "-jar",
          launchJar.getPath,
          "@" + lc.getPath
        ) ++ args

        val exit = command.run(BasicIO.standard(connectInput = true)).exitValue()

        if (exit == 0) {
          Success("Success!")
        } else {
          Failure(new RuntimeException(s"Failure, exit code: $exit"))
        }
    }.flatten

  def launch(template: String, g8Version: Option[String], args: Array[String]): Try[String] =
    for {
      lJar   <- SbtLaunchJar.get
      v      <- version(g8Version, template)
      lc     <- SbtLaunchConfig.get(v)
      result <- fetchAndRun(lJar, lc, args, v)
    } yield result
}

