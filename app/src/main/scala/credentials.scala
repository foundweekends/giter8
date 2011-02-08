package giter8

object SavedCreds {
  def apply(): Option[(String,String)] = {
    try {
      val fl = new java.io.File(System.getProperty("user.home"),
                                ".giter8-credentials")
      val p = new java.util.Properties()
      p.load( new java.io.BufferedReader(new java.io.FileReader(fl)) )
      Some((p.getProperty("username"), p.getProperty("password")))
    } catch { case _ => None }
  }
}
