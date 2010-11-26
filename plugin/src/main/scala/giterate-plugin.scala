package giter8

import sbt._

trait Template extends DefaultProject with Library {
  import java.io.File
  import scala.io.Source
  def templateSourcePath = (mainSourcePath / "g8") ##
  def templateSources = descendents(templateSourcePath, "*") --- defaultProperties

  def defaultProperties = templateSourcePath / "default.properties"

  def templateOutput = outputPath / "g8"

  override def cleanAction = super.cleanAction dependsOn cleanTask(templateOutput)

  lazy val sbtTest = task {
    import Process._
    (new java.lang.ProcessBuilder("sbt", "update", "test") directory 
        templateOutput.asFile)! match {
      case 0 => None
      case code => Some("failed to run `sbt update test` in %s with code %d" format 
                        (templateOutput, code))
    }
  } dependsOn writeTemplates describedAs 
    "Run `sbt update test` in %s to smoke-test the templates".format(templateOutput)

  lazy val writeTemplates = applyTemplates(
    templateSources,
    templateOutput,
    readProps(defaultProperties)
  ) describedAs "Apply default parameters to input templates and write to " + 
    templateOutput

  private def readProps(f: Path) = {
    val p = new java.util.Properties
    FileUtilities.readStream(f.asFile, log) { stm =>
      p.load(stm)
      None
    }
    p
  }
}
