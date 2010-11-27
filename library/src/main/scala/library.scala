package giter8

import sbt._

import scala.collection.jcl.Conversions._
import org.antlr.stringtemplate.StringTemplate
import java.util.{Map=>JMap}

trait Library extends FileTasks {
  def applyTemplates(fromFinder: PathFinder, toPath: Path, params: Map[_,_]): Task = {
    val jParams = new java.util.HashMap[Any,Any]()
    for ( (k, v) <- params.elements) jParams.put(k, v)
    applyTemplates(fromFinder, toPath, jParams)
  }
  def applyTemplates(fromFinder: PathFinder, toPath: Path, params: JMap[_,_]) = 
    fileTask(toPath from fromFinder) {
      val inputs = fromFinder.get.filter { !_.isDirectory }
      ((None: Option[String]) /: inputs) { (c, in) =>
        c orElse applyTemplate(in, Library.expandPath(in, toPath, params), params)
      } orElse FileUtilities.touch(toPath, log)
    }
  def applyTemplate(in: Path, out: Path, params: JMap[_,_]) = {
    log.debug("Applying " + in)
    FileUtilities.readString(in.asFile, log).right.map { str =>
      val st = new StringTemplate
      var parseError = false
      st.setErrorListener(new org.antlr.stringtemplate.StringTemplateErrorListener {
        def error(msg: String, exc: Throwable) { parseError = true }
        def warning(msg: String) { }
      })
      st.setTemplate(str)
      if (parseError) {
        log.info("Unable to parse template %s, copying unmodified" format in)
        FileUtilities.copyFile(in, out, log)
      } else {
        st.setAttributes(params: JMap[_,_])
        FileUtilities.write(out.asFile, st.toString, log)
      }
    }.left.toOption
  }
}
object Library {
  def expandPath(p: Path, toPath: Path, params: JMap[_,_]) = {
    val out = new StringTemplate(p.relativePath)
    out.setAttributes(params)
    Path.fromString(toPath, out.toString)
  }
}
