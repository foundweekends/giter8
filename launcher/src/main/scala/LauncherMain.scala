package giter8

import java.net.URLClassLoader
import java.io.{BufferedInputStream, File, FileInputStream}
import java.util.Properties
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Files, StandardCopyOption}

object LauncherMain extends Runner with App {
  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)

  System.exit(run(args, new LauncherProcessor))
}

class LauncherProcessor extends Processor {
  implicit private class RichFile(file: File) {
    def /(child: String): File = new File(file, child)
  }

  def process(
      templateDirectory: File,
      workingDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean,
      outputDirectory: Option[File]
  ): Either[String, String] = {
    val fallback = giter8.BuildInfo.version
    val g8v = giter8Version(templateDirectory) match {
      case Some(v) => v
      case _       => fallback
    }
    val virtualArgument: Vector[String] =
      Vector(templateDirectory.toPath.toUri.toString) ++
        arguments.toVector ++
        (if (forceOverwrite) Vector("-f") else Vector()) ++
        (outputDirectory match { case Some(out) => Vector("-o", out.toString); case _ => Vector() })
    val giter8Files = giter8Artifacts(g8v)
    virtuallyRun(giter8Files, virtualArgument, workingDirectory)
    Right("")
  }

  /** See if there are JARs in ~/.giter8/boot/org.foundweekends.giter8/giter8_2.12/0.10.x. Otherwise, use Coursier to
    * download the giter8 artifacts and move them into the boot dir for the next time.
    */
  def giter8Artifacts(g8v: String): Seq[File] = {
    val launcherVersion = "1.1.3"
    val bootHome        = Home.home / "boot"
    val bootDir         = bootHome / "org.foundweekends.giter8" / "giter8_2.12" / g8v
    val bootJars =
      if (bootDir.exists) bootDir.listFiles.toList filter { _.getName.endsWith(".jar") }
      else Nil
    val giter8Files0 =
      if (bootJars.nonEmpty) bootJars
      else {
        Console.err.println(s"[info] resolving Giter8 $g8v...")
        import coursier._
        val downloadedJars = Fetch()
          .addDependencies(
            Dependency(Module(Organization("org.foundweekends.giter8"), ModuleName("giter8_2.12")), g8v),
            Dependency(Module(Organization("org.scala-sbt"), ModuleName("launcher")), launcherVersion)
          )
          .run()
        if (!bootDir.exists) {
          Files.createDirectories(bootDir.toPath)
        }
        downloadedJars map { downloaded =>
          val t = bootDir / downloaded.getName
          Files.copy(downloaded.toPath, t.toPath, StandardCopyOption.REPLACE_EXISTING).toFile
        }
      }
    // push launcher JAR to the end of classpath to avoid Scala version clash
    val (a, b)      = giter8Files0.partition(_.getName.startsWith("launcher-"))
    val giter8Files = b ++ a
    giter8Files
  }

  def forkRun(files: Seq[File], args: Seq[String]): Unit = {
    import scala.sys.process._
    val classpath = files.map(_.getAbsolutePath).mkString(File.pathSeparator)
    Process("java", Seq("-cp", classpath, "giter8.Giter8") ++ args).run(connectInput = true).exitValue()
  }

  // uses classloader trick to run
  def virtuallyRun(files: Seq[File], args: Seq[String], workingDirectory: File): Unit = {
    val cl = new URLClassLoader(files.map(_.toURL).toArray, null)
    call("giter8.Giter8", "run", cl)(classOf[Array[String]], classOf[File])(args.toArray, workingDirectory)
  }

  def giter8Version(templateDir: File): Option[String] = {
    val prop = templateDir / "project" / "build.properties"
    if (prop.exists) {
      val props = new Properties()
      val fis   = new BufferedInputStream(new FileInputStream(prop))
      try {
        props.load(fis)
      } finally {
        fis.close()
      }
      Option(props.getProperty("giter8.version"))
    } else None
  }

  private def call(
      interfaceClassName: String,
      methodName: String,
      loader: ClassLoader
  )(argTypes: Class[_]*)(args: AnyRef*): AnyRef = {
    val interfaceClass = getInterfaceClass(interfaceClassName, loader)
    val interface      = interfaceClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    val method         = interfaceClass.getMethod(methodName, argTypes: _*)
    try {
      method.invoke(interface, args: _*)
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }

  private def getInterfaceClass(name: String, loader: ClassLoader) =
    Class.forName(name, true, loader)
}
