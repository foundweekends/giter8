/** copied from https://github.com/harrah/xsbt/blob/0.11/scripted/plugin/ScriptedPlugin.scala.
 * since ScriptedPlugin is not within a package, it cannot be reused from a packaged class.
 */

package giter8

import sbt._

import Project.Initialize
import Keys._
import classpath.ClasspathUtilities
import java.lang.reflect.Method
import java.util.Properties

object Scripted {
  def scriptedConf = config("g8-scripted-sbt") hide

  val scriptedSbt = SettingKey[String]("_g8-scripted-sbt")
  val sbtLauncher = SettingKey[File]("_g8-sbt-launcher")
  val sbtTestDirectory = SettingKey[File]("_g8-sbt-test-directory")
  val scriptedBufferLog = SettingKey[Boolean]("_g8-scripted-buffer-log")
  final case class ScriptedScalas(build: String, versions: String)
  val scriptedScalas = SettingKey[ScriptedScalas]("_g8-scripted-scalas")

  val scriptedClasspath = TaskKey[PathFinder]("_g8-scripted-classpath")
  val scriptedTests = TaskKey[AnyRef]("_g8-scripted-tests")
  val scriptedRun = TaskKey[Method]("_g8-scripted-run")
  val scriptedDependencies = TaskKey[Unit]("_g8-scripted-dependencies")
  val scripted = InputKey[Unit]("_g8-scripted")   
  
  def scriptedTestsTask: Initialize[Task[AnyRef]] = (scriptedClasspath, scalaInstance) map {
    (classpath, scala) =>
    val loader = ClasspathUtilities.toLoader(classpath, scala.loader)
    ModuleUtilities.getObject("sbt.test.ScriptedTests", loader)
  }

  def scriptedRunTask: Initialize[Task[Method]] = (scriptedTests) map {
    (m) =>
    m.getClass.getMethod("run", classOf[File], classOf[Boolean], classOf[String], classOf[String], classOf[String], classOf[Array[String]], classOf[File]) 
  }
  
  def scriptedTask: Initialize[InputTask[Unit]] = InputTask(_ => complete.Parsers.spaceDelimited("<arg>")) { result =>
    (scriptedDependencies, scriptedTests, scriptedRun, sbtTestDirectory, scriptedBufferLog, scriptedSbt, scriptedScalas, sbtLauncher, result) map {
      (deps, m, r, testdir, bufferlog, version, scriptedScalas, launcher, args) =>
      try { r.invoke(m, testdir, bufferlog: java.lang.Boolean, version.toString, scriptedScalas.build, scriptedScalas.versions, args.toArray, launcher) }
      catch { case e: java.lang.reflect.InvocationTargetException => throw e.getCause }
    }
  }   
  
  lazy val scriptedSettings: Seq[sbt.Project.Setting[_]] = Seq(
    ivyConfigurations += scriptedConf,
    resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
    scriptedSbt <<= (appConfiguration)(_.provider.id.version),
    scriptedScalas <<= (scalaVersion) { (scala) => ScriptedScalas(scala, scala) },
    libraryDependencies <<= (libraryDependencies, scriptedScalas, scriptedSbt) {(deps, scalas, version) => deps :+ "org.scala-sbt" % ("scripted-sbt_" + scalas.build) % version % scriptedConf.toString },
    sbtLauncher <<= (appConfiguration)(app => IO.classLocationFile(app.provider.scalaProvider.launcher.getClass)),
    sbtTestDirectory <<= sourceDirectory / "sbt-test",
    scriptedBufferLog := true,
    scriptedClasspath <<= (classpathTypes, update) map { (ct, report) => PathFinder(Classpaths.managedJars(scriptedConf, ct, report).map(_.data)) },
    scriptedTests <<= scriptedTestsTask,
    scriptedRun <<= scriptedRunTask,
    scriptedDependencies <<= (compile in Test, publishLocal) map { (analysis, pub) => Unit },
    scripted <<= scriptedTask     
  )
}
