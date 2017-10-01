package giter8.launcher

import java.io.{File, FileInputStream}
import java.util.Properties

import giter8.{TempDir, _}
import org.apache.commons.io.FileUtils

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Launcher extends TempDir {

  import giter8.G8.RichFile
  import giter8.OptTry

  private val git = new Git(new JGitInteractor())

  /**
    * Use JAVA_HOME as a default location for finding a java executable. If
    * it isn't set fall back to the `java.home` of the current JVM
    */
  private lazy val javaHome: String =
    sys.env.getOrElse("JAVA_HOME", System.getProperty("java.home"))

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
  private def version(g8Version: Option[String], template: String): Try[Option[VersionNumber]] =
    Try(g8Version.map(VersionNumber.apply)) or git.withRepo(template) {
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
    * Retrieves the launch config of a specific tagged version of Giter8,
    * if `version` is undefined, we attempt to retrieve the latest version
    *
    * NOTE: It seems that the version number in some of the launchconfigs
    * does not match the release version. (I think that this is due to 0.8.0 being
    * reverted, so it shouldn't be a problem)
    *
    * e.g. https://github.com/foundweekends/giter8/blob/v0.9.0/src/main/conscript/g8/launchconfig#L2
    *
    * @param version
    * @return
    */
  private def launchConfig(version: Option[VersionNumber]): Try[String] =
    git.withRepo("https://github.com/foundweekends/giter8", version.map(_.toString)) {
      base =>
        val lcFile = base / "src" / "main" / "conscript" / "g8" / "launchconfig"
        val result: String = Source.fromFile(lcFile).getLines.mkString("\r\n")
        println(s"LaunchConfig resolved for version: ${version.getOrElse("LATEST")}")
        result
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
                           lc: String,
                           args: Array[String],
                           v: Option[VersionNumber]
                         ): Try[String] =
    withTempdir {
      base =>

        import scala.sys.process._

        val lcFile: File = base / "launchconfig"
        FileUtils.write(lcFile, lc)

        val java: File =
          new File(javaHome) / "bin" / "java"

        println(s"Fetching Giter8 ${v.getOrElse("LATEST")}")

        val command = Seq(
          java.getPath, "-DGITER8_FORKED=true", "-jar",
          launchJar.getPath,
          "@" + lcFile.getPath
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
      lc     <- launchConfig(v)
      result <- fetchAndRun(lJar, lc, args, v)
    } yield result
}

