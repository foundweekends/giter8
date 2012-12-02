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
    val in = new FileInputStream(file)
    p.load(in)
    in.close()

    val result = f(p)
    val out = new FileOutputStream(file)
    p.store(out, null)
    out.close()
    result
  }
}
