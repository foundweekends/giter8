package giter8

import java.io.File
import giter8.G8.RichFile

object SystemPaths {

  def javaHome: File = getSystemPath("JAVA_HOME", file(System.getProperty("java.home")))

  def base: File = getSystemPath("giter8.base", userHome / ".giter8")

  def launchJar: File = getSystemPath("giter8.launch.jar", base / "launch" / "sbt-launch.jar")

  def launchJarUrl: String = {
    val url = "https://repo1.maven.org/maven2/org/scala-sbt/launcher/1.0.1/launcher-1.0.1.jar"
    sys.env.getOrElse("giter8.launch.jar.url", url)
  }

  def launchConfigBase: File = getSystemPath("giter8.launch.config", base / "launch" / "config")

  private def getSystemPath(property: String, default: File): File = {
    sys.env
      .get(property)
      .map(file)
      .getOrElse(default)
  }

  private lazy val userHome: File =
    file(System.getProperty("user.home"))
}
