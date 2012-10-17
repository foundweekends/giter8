package giter8

object GIO {
  import java.io.{File,
                  FileInputStream => FIS,
                  InputStream => IS,
                  FileOutputStream => FOS,
                  OutputStream => OS,
                  ByteArrayOutputStream => BOS,
                  ByteArrayInputStream => BIS
                }

  def use[C<:{ def close(): Unit}, T](c: C)(f: C => T) = try {
    f(c)
  } finally {
    c.close
  }

  @annotation.tailrec
  def transfer(fis: IS, fos: OS, buf: Array[Byte]) {
    fis.read(buf, 0, buf.length) match {
      case -1 =>
        fos.flush()
      case read =>
        fos.write(buf, 0, read)
        transfer(fis, fos, buf)
    }
  }

  def read(from: File, charset: String) = {
    val bos = new BOS()
    use(new FIS(from)) { in =>
      use(bos) { out =>
        transfer(in, out, new Array[Byte](1024*16))
      }
    }
    bos.toString(charset)
  }    
  def copyFile(from: File, to: File, append: Boolean = false) {
    to.getParentFile().mkdirs()
    use(new FIS(from)) { in =>
      use(new FOS(to, append)) { out =>
        transfer(in, out, new Array[Byte](1024*16))
      }
    }
  }
  def write(to: File, from: String, charset: String, append: Boolean = false) {
    to.getParentFile().mkdirs()
    use(new BIS(from.getBytes(charset))) { in =>
      use(new FOS(to, append)) { out =>
        transfer(in, out, new Array[Byte](1024*16))
      }
    }
  }
  def readProps(stm: java.io.InputStream) = {
    import scala.collection.JavaConversions._
    val p = new java.util.Properties
    p.load(stm)
    stm.close()
    (Map.empty[String, String] /: p.propertyNames) { (m, k) =>
      m + (k.toString -> p.getProperty(k.toString))
    }
  }
}
