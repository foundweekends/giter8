package giter8

import java.io.File

trait TemplateRenderer {
  def render(baseDirectory: File, outputDirectory: File, arguments: Seq[String], forceOverwrite: Boolean): Either[String, String]
}

object G8TemplateRenderer extends TemplateRenderer {
  override def render(baseDirectory: File, outputDirectory: File, arguments: Seq[String], forceOverwrite: Boolean): Either[String, String] =
    G8.fromDirectory(baseDirectory, outputDirectory, arguments, forceOverwrite)
}
