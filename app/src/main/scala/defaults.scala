package giter8

trait Defaults { self: Giter8 =>
  def prepareDefaults(
    repo: String,
    properties: Option[FileInfo]
  ) = Ls.lookup(fetchDefaults(repo, properties))

  def fetchDefaults(repo: String, properties: Option[FileInfo]) =
    properties.map { fileinfo =>
      http(show(repo, fileinfo.hash) >> GIO.readProps _ )
    }.getOrElse { Map.empty }
}
