package giter8.launcher

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

import gigahorse.support.okhttp.Gigahorse
import org.apache.commons.io.FileUtils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import giter8._

object SbtLaunchJar {

  /**
    * Calculate the SHA1 checksum of an array of bytes
    *
    * @param bytes
    * @return
    */
  private def sha1sum(bytes: Array[Byte]): String = {
    val digest = MessageDigest.getInstance("SHA1").digest(bytes)
    new BigInteger(1, digest).toString(16).reverse.padTo(40, "0").reverse.mkString
  }

  /**
    * Retrieve the SHA1 checksum for a particular file in Maven Central
    *
    * @param url
    * @return
    */
  private def getRemoteSum(url: String): Try[String] = {
    Gigahorse.withHttp(Gigahorse.config) {
      http =>

        val request = Gigahorse.url(url + ".sha1").get
        val response = http.run(request).map(_.bodyAsString)

        // TODO?: Configurable timeout
        Try(Await.result(response, 1.minute))
    }
  }

  /**
    * Calculate the SHA1 checksum of a file
    *
    * @param file
    * @return
    */
  private def getLocalSum(file: File): Try[String] =
    Try(FileUtils.readFileToByteArray(file)).map(sha1sum)

  /**
    * Fetch a jar from a url, checking it against the checksum associated
    * with that url
    *
    * @param url
    * @param file
    * @param checksum
    * @return
    */
  private def fetchJar(url: String, file: File, checksum: Option[String] = None): Try[File] = {
    Gigahorse.withHttp(Gigahorse.config) {
      http =>

        checksum.map(Success.apply).getOrElse(getRemoteSum(url)).flatMap {
          remoteSum =>

            FileUtils.touch(file)
            println("Fetching remote jar...")
            val request = Gigahorse.url(url).get
            val response = http.run(request).map {
              r =>
                val bytes = r.bodyAsByteBuffer.array
                val localSum = sha1sum(bytes)
                if (localSum == remoteSum) {
                  println("Downloaded jar is valid!")
                  Try(FileUtils.writeByteArrayToFile(file, bytes))
                    .map(_ => file)
                } else {
                  Failure(new RuntimeException("Downloaded jar is invalid... exiting"))
                }
            }

            Try(Await.result(response, 10.minutes))
        }.flatten match {
          case failure @ Failure(e) =>
            e.printStackTrace()
            FileUtils.forceDeleteOnExit(file)
            failure
          case tried => tried
        }
    }
  }

  /**
    * Check a cached version of a file against a checksum, if the file doesn't exist,
    * or if it's invalid, re-download the file and cache it again
    *
    * @param url
    * @return
    */
  private def fetchAndCacheJar(url: String, file: File): Try[File] = {

    getRemoteSum(url).flatMap {
      remoteSum =>

        if (file.exists) {
          println("Found cached jar")
          getLocalSum(file).flatMap {
            localSum =>
              if (localSum == remoteSum) {
                println("Cached jar is valid!")
                Success(file)
              } else {
                println("Cached jar is invalid...")
                fetchJar(url, file, Some(remoteSum))
              }
          }
        } else {
          fetchJar(url, file, Some(remoteSum))
        }
    }
  }

  lazy val get: Try[File] = {
    fetchAndCacheJar(SystemPaths.launchJarUrl, SystemPaths.launchJar)
  }
}
