package giter8

import sbt._

trait Template extends DefaultProject {
  import java.io.File
  import scala.io.Source
  import scala.collection.jcl.Conversions._
  import org.antlr.stringtemplate.StringTemplate
  def templateSourcePath = (mainSourcePath / "g8") ##
  def templateSources = descendents(templateSourcePath, "*")

  def defaultProperties = templateSourcePath / "default.properties"

  def templateOutput = outputPath / "g8"

  override def cleanAction = super.cleanAction dependsOn cleanTask(templateOutput)

  lazy val sbtTest = task {
    import Process._
    (new java.lang.ProcessBuilder("sbt", "update", "compile") directory templateOutput.asFile) ! match {
      case 0 => None
      case code => Some("failed to run `sbt update compile` in %s with code %d" format (templateOutput, code))
    }
  } dependsOn writeTemplates

  lazy val writeTemplates = fileTask(templateOutput from templateSources) {
    templateSources.get.filter(!_.isDirectory).partition { _ == defaultProperties } match {
      case (props, inputs) =>
        val params = props.map(readProps).find(_ => true).getOrElse(new java.util.HashMap)
        ((None: Option[String]) /: inputs) { (c, in) =>
          c orElse writeTemplate(in, Path.fromString(templateOutput, in.relativePath), params)
        } orElse FileUtilities.touch(templateOutput, log)
    }
  }

  private def readProps(f: Path) = {
    val p = new java.util.Properties
    FileUtilities.read(f.asFile, log) { reader =>
      p.load(reader)
      None
    }
    p
  }

  def writeTemplate(in: Path, out: Path, params: java.util.Map[_,_]) =
    FileUtilities.write(out.asFile, log) { writer =>
      val st = new StringTemplate(
        Source.fromFile(in.asFile).mkString("")
      )
      st.setAttributes(params)
      writer.write(st.toString)
      None
    }
}
