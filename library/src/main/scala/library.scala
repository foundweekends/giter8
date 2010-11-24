package giter8

import sbt._

import scala.collection.jcl.Conversions._
import org.antlr.stringtemplate.StringTemplate
import java.util.{Map=>JMap}

trait Library extends FileTasks {
  def applyTemplates(fromFinder: PathFinder, toPath: Path, params: JMap[_,_]) = 
    fileTask(toPath from fromFinder) {
      val inputs = fromFinder.get.filter { !_.isDirectory }
      ((None: Option[String]) /: inputs) { (c, in) =>
        c orElse applyTemplate(in, Library.expandPath(in, toPath, params), params)
      } orElse FileUtilities.touch(toPath, log)
    }
  def applyTemplate(in: Path, out: Path, params: JMap[_,_]) =
    FileUtilities.write(out.asFile, log) { writer =>
      val st = new StringTemplate(
        scala.io.Source.fromFile(in.asFile).mkString("")
      )
      st.setAttributes(params: JMap[_,_])
      writer.write(st.toString)
      None
    }
}
object Library {
  def expandPath(p: Path, toPath: Path, params: JMap[_,_]) = {
    val out = new StringTemplate(p.relativePath)
    out.setAttributes(params)
    Path.fromString(toPath, out.toString)
  }
}
