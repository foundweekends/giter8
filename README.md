`g8` -- executable script on path:

    sbt @.giter8.launchconfig "$@"

`.giter8.launchconfig` -- in your home directory:

    [scala]
      version: 2.8.0
    [app]
      org: net.databinder
      name: giter8
      version: 0.1.0
      class: giter8.Giter8
    [repositories]
      local
      scala-tools-releases
    [boot]
      directory: /path/to/home/.giter8/boot