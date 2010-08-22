giter8
====

Giter8 is a command line tool to generate files and directories from
templates published on github. It's implemented in Scala and runs
through the [Simple Build Tool launcher][launcher], but it
can produce output for any purpose.

[launcher]: http://code.google.com/p/simple-build-tool/wiki/GeneralizedLauncher
 
Installation
---------

If you haven't already setup sbt, you need to [go do that][sbt]. Then...

[sbt]: http://code.google.com/p/simple-build-tool/wiki/Setup

Make sure that the `sbt` is on your path. You should be able to run it
from any directory and be prompted to create a new project (and then abort).

You need to create a new `g8` script also on your executable path. For 
unix-like shells, it should contain:

    #!/bin/sh
    sbt @.giter8.launchconfig "$@"

And it should be executable:

    $ chmod a+x g8

The parameter passed to sbt tells its launcher to use the given launch
configuration instead of starting sbt itself. The launcher will look for the
configuration in several places; one of these is your home directory, and
giter8's launch configuration is prefixed with a dot so that you can store
it there without it being all up in your face.

Here is a launch configuration for the current version of
giter8.  You can paste it into a file `~/.giter8.launchconfig`

    [app]
      version: 0.1.1
      org: net.databinder
      name: giter8
      class: giter8.Giter8
    [scala]
      version: 2.8.0
    [repositories]
      local
      maven-local
      scala-tools-releases
      maven-central
      clapper: http://maven.clapper.org/
    [boot]
      directory: /path/to/home/.giter8/boot

There is one thing you need to change in it, however! The last line
specifies the "boot" directory, where versions of giter8 will be
downloaded and stored. You may keep these anywhere that your user
account is permitted to write; we recommend using `.giter8/boot`
under your home directory. Note that *tilde (~) is not supported* by the launcher
so you'll need to enter the full path.

To make sure everything is working, try running `g8` with no
parameters. It should download giter8 and its dependencies, then print
a usage message.

When it's time to upgrade to a new version of giter8, you'll only need
to adjust the version number in `.giter8.launcher`.

Usage
-----

Template repositories must reside on github and be named with the 
suffix ".g8". For example, the repo [softprops/unfiltered.g8][uft] is
a giter8 template. You can apply it from the command line like so:

[uft]: http://github.com/softprops/unfiltered.g8

    $ g8 softprops/unfiltered.g8

You can also drop the suffix and it will be assumed:

    $ g8 softprops/unfiltered

Either way, giter8 resolves this to the softprops/unfiltered.g8
repository and queries github for the project's template
parameters. You'll be prompted for each parameter, with its default
value in square brackets:

    name [My Web Project]: 

Enter your own value or press enter to accept the default. After all
the values have been supplied, giter8 fetches the templates, applies
the parameters, and writes them to your filesystem. If the template
has a "name" parameter it will be used to create base directory within
the current directory. (Typically, this is the base directory for a new
project). If the template does not have a "name" parameter, giter8
will output its files and directories within the current directory,
skipping over  any files that already exist.

Once you become familiar with a template's parameters, you can enter
them on the command line and skip the interaction:

    $ g8 softprops/unfiltered.g8 --name=my-new-website

Any parameters that are not supplied will be assigned their default
values.

Questions that will probably be frequent
----------------------------------

### Isn't this like Lifty?

Nope. [Lifty] is an [sbt processor][processor], meaning it runs inside
of sbt itself. You can't run sbt or any processor until you have a
project to run it in. Giter8 addresses step 1 of sbt project
creation. You could use giter8 create a Lift project, then run Lifty
inside it for fine tuning. You can also use giter8 to produce things
that are not sbt projects at all.

[Lifty]: http://lifty.github.com/
[processor]: http://code.google.com/p/simple-build-tool/wiki/Processors

### Neat, how can I make my own templates?

Use the template-template:

    $ g8 n8han/giter8

This will create an sbt project with the template sources nested under
`src/main/g8`. It's an sbt project so that you can use sbt to apply
and test your template locally, before pushing it to github. This
process needs docs, but if you are good with sbt you can probably
figure it out. [StringTemplate][st], wrapped by [Scalasti][scalasti], is
the engine for giter8 templates. Good luck.

[scalasti]: http://bmc.github.com/scalasti/
[st]: http://www.stringtemplate.org/
