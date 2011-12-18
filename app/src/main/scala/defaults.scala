package giter8

trait Defaults { self: Giter8 =>
  def prepareDefaults(
    repo: String,
    properties: Option[FileInfo]
  ) = {
    val rawDefaults = fetchDefaults(repo, properties)
    val lsDefaults = rawDefaults.view.collect {
      case (key, Ls(library, user, repo)) =>
        ls.DefaultClient {
          _.Handler.latest(library, user, repo)
        }.right.map { key -> _ }
    }
    val initial: Either[String,Map[String,String]] = Right(rawDefaults)
    (initial /: lsDefaults) { (accumEither, lsEither) =>
      for {
        cur <- accumEither.right
        ls <- lsEither.right
      } yield cur + ls
    }.left.map { "Error retrieving ls version info: " + _ }
  }

  def fetchDefaults(repo: String, properties: Option[FileInfo]) =
    properties.map { fileinfo =>
      http(show(repo, fileinfo.hash) >> readProps _ )
    }.getOrElse { Map.empty }

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
