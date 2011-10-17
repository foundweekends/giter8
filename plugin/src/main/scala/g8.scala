package giter8

import sbt._

object G8 {
  import scala.collection.JavaConversions._
  import org.antlr.stringtemplate.StringTemplate
  import java.util.{Map=>JMap}
  
  def apply(fromMapping: Seq[(File,String)], toPath: File, params: Map[_,_], log: Logger): Seq[File] = {
    val jParams = new java.util.HashMap[Any,Any]()
    for { (k, v) <- params.elements } jParams.put(k, v)
    apply(fromMapping, toPath, jParams, log)
  }
  def apply(fromMapping: Seq[(File,String)], toPath: File, params: JMap[_,_], log: Logger): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      apply(in, expandPath(relative, toPath, params), params, log)
    }  
  def apply(in: File, out: File, params: JMap[_,_], log: Logger): Seq[File] = {
    log.debug("Applying " + in)
    val st = new StringTemplate
    var parseError = false
    st.setErrorListener(new org.antlr.stringtemplate.StringTemplateErrorListener {
      def error(msg: String, exc: Throwable) { parseError = true }
      def warning(msg: String) { }
    })
    st.setTemplate(IO.read(in))
    if (parseError) {
      log.info("Unable to parse template %s, copying unmodified" format in)
      IO.copyFile(in, out)
    } else {
      st.setAttributes(params: JMap[_,_])
      IO.write(out, st.toString)
    }
    Seq(out)
  }
  private def expandPath(relative: String, toPath: File, params: JMap[_,_]): File = {
    val out = new StringTemplate(relative)
    out.setAttributes(params)
    new File(toPath, out.toString)
  }
}
