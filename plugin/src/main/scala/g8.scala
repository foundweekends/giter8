package giter8

import sbt._

object G8 {
  import scala.util.control.Exception.allCatch
  import org.clapper.scalasti.StringTemplate
  
  def apply(fromMapping: Seq[(File,String)], toPath: File, parameters: Map[String,String], log: Logger): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      apply(in, expandPath(relative, toPath, parameters), parameters, log)
    }  
  def apply(in: File, out: File, parameters: Map[String,String], log: Logger): Seq[File] = {
    log.debug("Applying " + in)
    import java.nio.charset.{MalformedInputException, Charset}
    
    allCatch opt {
      val template = IO.read(in, Charset forName "UTF-8")
      IO.write(out, new StringTemplate(template).setAttributes(parameters).toString)    
    } getOrElse {
      log.info("Unable to parse template %s, copying unmodified" format in)
      IO.copyFile(in, out)      
    }
    allCatch opt {
      if (in.canExecute) out.setExecutable(true)
    }
    Seq(out)
  }
  private def expandPath(relative: String, toPath: File, parameters: Map[String,String]): File = {
    val fileParams = Map(parameters.toSeq map {
      case (k, v) if k == "package" => (k, v.replaceAll("""\.""", System.getProperty("file.separator")))
      case x => x
    }: _*)
    
    new File(toPath, new StringTemplate(relative).setAttributes(fileParams).toString)
  }
}
