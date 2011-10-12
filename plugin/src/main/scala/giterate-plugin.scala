package giter8

import sbt._

object Plugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source
  
  object G8Keys {
    lazy val g8                = TaskKey[Seq[File]]("g8", "Apply default parameters to input templates and write to output.")
    lazy val outputPath        = SettingKey[File]("g8-output-path")
    lazy val propertiesFile    = SettingKey[File]("g8-properties-file")
    lazy val properties        = SettingKey[Map[Any, Any]]("g8-properties")
    lazy val sbtTest           = TaskKey[Unit]("g8-sbt-test", "Run `sbt test` in output to smoke-test the templates")
  }
  
  import G8Keys._
  
  lazy val baseGiter8Settings: Seq[sbt.Project.Setting[_]] = Seq(
    g8 <<= (unmanagedSourceDirectories in g8,
        sources in g8, outputPath in g8,
        properties in g8, streams) map { (base, srcs, out, props, s) =>
      IO.delete(out)
      G8(srcs x relativeTo(base), out, props, s.log) },
    unmanagedSourceDirectories in g8 <<= (sourceDirectory) { dir => (dir / "g8").get },
    sources in g8 <<= (unmanagedSourceDirectories in g8, propertiesFile in g8) map { (dirs, pf) =>
      ((dirs ** (-DirectoryFilter)) --- pf).get },
    outputPath in g8 <<= (target) { dir => dir / "g8" },
    propertiesFile in g8 <<= (unmanagedSourceDirectories in g8) { dirs => (dirs / "default.properties").get.head },
    properties in g8 <<= (propertiesFile in g8) { f =>
      import scala.collection.JavaConversions._
      val p = new java.util.Properties
      p.load(new java.io.ByteArrayInputStream(IO.readBytes(f)))
      Map((for { k <- p.propertyNames } yield (k.toString, p.getProperty(k.toString))).toSeq:_*)    
    },   
    sbtTest <<= (g8, outputPath in g8) map { (g8, outputPath) =>
      import Process._
      (new java.lang.ProcessBuilder("sbt", "test") directory outputPath)! match {
        case 0 => None
        case code => error("failed to run `sbt update test` in %s with code %d" format 
                          (outputPath, code))
      }
    }
  )
  lazy val giter8Settings: Seq[sbt.Project.Setting[_]] = inConfig(Compile)(baseGiter8Settings)
}
