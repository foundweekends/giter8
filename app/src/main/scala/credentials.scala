package giter8

trait Credentials { self: Apply =>
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
    Option(System.getenv(key.toUpperCase.replaceAll("""\.""", "_"))) map { Some(_) } getOrElse {
      val p = new java.lang.ProcessBuilder("git", "config", "--global", key).start()
      val reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream))
      Option(reader.readLine)
    }
}
