package giter8

trait Credentials { self: Apply =>
  def withCredentials(req: dispatch.Request) =
    credentials map { case (user, pass) => req as_! (user, pass) } getOrElse req

  lazy val credentials = {
    val props = Some(new java.io.File(System.getProperty("user.home"), ".gh")) filter {
      _.exists
    } map { f => readProps(new java.io.FileInputStream(f)) } getOrElse Map.empty
    props.get("username") flatMap { user =>
      props.get("token") map { token => (user + "/token", token) } orElse {
        props.get("password") map { pass => (user, pass) }
      }
    }
  }
}
