object IO {
  import java.io.{File, FileInputStream => FIS, FileOutputStream => FOS}

  def use[C<:{ def close(): Unit}, T](c: C)(f: C => T) = try {
    f(c)
  } finally {
    c.close
  }

  def copyFile(from: File, to: File) = {
    to.getParentFile().mkdirs()
    @annotation.tailrec
    def transfer(fis: FIS, fos: FOS, buf: Array[Byte]): Unit = {
      fis.read(buf, 0, buf.length) match {
        case -1 =>
          fos.flush()
        case read =>
           fos.write(buf, 0, read)
           transfer(fis, fos, buf)
      }
    }
    use(new FIS(from)) { in =>
      use(new FOS(to)) { out =>
        transfer(in, out, new Array[Byte](1024*16))
      }
    }
  }
}
