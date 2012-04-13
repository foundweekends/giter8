package giter8

object Config {
  import java.io.{ File, FileInputStream, FileOutputStream }
  import java.util.Properties
  
  val file = new File(Giter8.home, "config")

  def get(name: String) =
    Option(properties {
      _.getProperty(name)
    })

  def properties[A](f: Properties => A): A = {
    if (!file.exists()) {
      file.getParentFile().mkdirs()
      file.createNewFile()
    }
    val p = new Properties()
    GIO.use(new FileInputStream(file)) { in =>
      p.load(in)
    }
    val result = f(p)
    GIO.use(new FileOutputStream(file)) { out =>
      p.store(out, null)
    }
    result
  }
}
