package giter8

trait Credentials { self: Apply =>
  import scala.util.control.Exception.allCatch
  
  def withCredentials(req: dispatch.Request) =
    credentials map { case (user, pass) => req as_! (user, pass) } getOrElse req
  
  lazy val credentials: Option[(String, String)] =
    gitConfig("github.user") flatMap { user =>
      gitConfig("github.token") map { token =>
        (user + "/token", token)
      }    
    }
  
  // https://github.com/defunkt/gist/blob/master/lib/gist.rb#L237
  def gitConfig(key: String): Option[String] =
    allCatch opt {
      Option(System.getenv(key.toUpperCase.replaceAll("""\.""", "_"))) map { Some(_) } getOrElse {
        val gitExec = windows map {_ => "git.exe"} getOrElse {"git"}
        val p = new java.lang.ProcessBuilder(gitExec, "config", "--global", key).start()
        val reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream))
        Option(reader.readLine)
      }
    } getOrElse {None}
  
  def windows =
    System.getProperty("os.name") match {
      case x: String if x contains "Windows" => Some(x)
      case _ => None
    }
}
