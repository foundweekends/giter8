package giter8

import java.io.{File, FileInputStream}
import java.util.Properties

import org.apache.commons.io.FileUtils

import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

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
    * If Conscript is installed, we can use its launch jar
    */
  private lazy val conscriptHome: Option[File] =
    sys.env.get("CONSCRIPT_HOME").map(p => new File(p))

  /**
    * Allow overriding the location of the launch jar
    */
  private lazy val launchJarProp: Option[File] =
    sys.env.get("SBT_LAUNCH_JAR").map(p => new File(p))

  /**
    * Fallback for if no launch jar is configured. Downloads the jar from
    * Maven Central and caches it in a temp directory.
    */
  private lazy val mavenLaunchJar: Try[File] = {

    val file = new File(System.getProperty("java.io.tmpdir")) / "giter8-launcher" / "launcher-1.0.1.jar"

    println("Locating launcher")
    if (file.exists) {
      println("Found cached launcher")
      Success(file)
    } else {
      println("Fetching launcher from Maven Central")
      val response = Http("https://repo1.maven.org/maven2/org/scala-sbt/launcher/1.0.1/launcher-1.0.1.jar").asBytes
      if (response.code == 200) {
        Try {
          FileUtils.writeByteArrayToFile(file, response.body)
          println("Successfully retrieved launcher from Maven Central")
          file
        }
      } else {
        Failure(new RuntimeException(s"Request to retrieve `sbt-launcher` failed: ${response.code}"))
      }
    }
  }

  /**
    * Use JAVA_HOME as a default location for finding a java executable.
    */
  private lazy val javaHome: Option[String] =
    sys.env.get("JAVA_HOME")

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

  private lazy val sbtLaunchJar: Try[File] = {
    (launchJarProp orElse conscriptHome.map(_ / "sbt-launch.jar"))
      .map(Success.apply)
      .getOrElse(mavenLaunchJar)
  }

  /**
    * Creates another JVM process running Conscript's `sbt-launch.jar` with the resolved
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
                           v: Option[String]
                         ): Try[String] =
    withTempdir {
      base =>

        import scala.sys.process._

        val lcFile: File = base / "launchconfig"
        FileUtils.write(lcFile, lc)

        /**
          *  Attempt to use JAVA_HOME to find a java executable, if
          *  no JAVA_HOME is defined, use `java.home` of the current JVM
          */
        val java: String =
          (new File(javaHome getOrElse System.getProperty("java.home")) / "bin" / "java").getPath

        println(s"Fetching Giter8 ${v.getOrElse("LATEST")}")

        // TODO add "-r" flag for versions >= when this is released, as otherwise we may end up in a loop
        val command = Seq(java, "-jar", launchJar.getPath, s"@${lcFile.getPath}" /*, "-r" */) ++ filteredArgs(args)
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
      lJar   <- sbtLaunchJar
      v      <- version(g8Version, template)
      lc     <- launchConfig(v)
      result <- fetchAndRun(lJar, lc, args, v)
    } yield result
}

