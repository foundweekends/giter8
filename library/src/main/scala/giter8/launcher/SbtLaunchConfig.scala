package giter8.launcher

import java.io.File

import giter8.G8.RichFile
import giter8.{Git, JGitInteractor, SystemPaths, VersionNumber}
import org.apache.commons.io.FileUtils

import scala.util.{Success, Try}

object SbtLaunchConfig {

  private lazy val git = new Git(new JGitInteractor())

  /**
    * Attempt to look up launchconfig for `version` locally
    *
    * @param version
    * @return
    */
  private def localVersion(version: VersionNumber): Option[File] = {
    val file: File = SystemPaths.launchConfigBase / version.toString
    if (file.exists) {
      println(s"Found local launchconfig for version: $version")
      Some(file)
    } else {
      None
    }
  }

  /**
    * Fetch the launchconfig for `version` from the giter8 github repo.
    * Lookup checks out a tag of the `version` to download
    *
    * Cache the downloaded config locally
    *
    * @param version
    * @return
    */
  private def remoteVersion(version: Option[VersionNumber]): Try[File] = {
    val versionString = version.map(_.toString)
    git.withRepo("https://github.com/foundweekends/giter8", versionString) {
      base =>
        val file = base / "src" / "main" / "conscript" / "g8" / "launchconfig"
        val outFile = SystemPaths.launchConfigBase / versionString.getOrElse("LATEST")
        Try(FileUtils.copyFile(file, outFile)).map {
          _ =>
            println(s"Found remote launchconfig for version: ${version.getOrElse("LATEST")}")
            outFile
        }
    }.flatten
  }

  /**
    * Find the `File` instance for the launchconfig file for the current `version`.
    *
    * If `version` is `None` try to resolve the LATEST version from the master branch
    * of giter8.
    *
    * NOTE: `LATEST` will always resolve remotely.
    *
    * @param version
    * @return
    */
  def get(version: Option[VersionNumber]): Try[File] = {
    version
      .flatMap(localVersion)
      .map(Success.apply)
      .getOrElse(remoteVersion(version))
  }
}
