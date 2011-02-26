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
      version: 0.2.0
      org: net.databinder
      name: giter8
      class: giter8.Giter8
    [scala]
      version: 2.8.1
    [repositories]
      local
      maven-local
      scala-tools-releases
      maven-central
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

Giter8 is also installable with the OS X package manager [Homebrew][]:

    $ brew update && brew install giter8

[Homebrew]: http://mxcl.github.com/homebrew/

Usage
-----

Template repositories must reside on github and be named with the
suffix ".g8". We're keeping a [list of templates on the wiki][wiki],
and you can query github to list all templates with a ".g8" suffix
from the command line:

    $ g8 --list

To apply a template, for example, [softprops/unfiltered.g8][uft]:

[uft]: http://github.com/softprops/unfiltered.g8
[wiki]: http://github.com/n8han/giter8/wiki/giter8-templates

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

Private Repositories
--------------------

Giter8 accesses GitHub anonymously by default, but for private
templates you can supply a name and token or password in `~/.gh`:

    username=yourusername
    token=yourtoken

Making your own templates
-------------------------

The g8 runtime looks for templates in the `src/main/g8` directory of a
given github project. This structure is used so that it is easy (but
not required) for the template itself to be an sbt project. That way,
an sbt plugin can be employed to locally test templates before pushing
changes to github.

The easy way to start a new template project is with a giter8 template
made expressly for that purpose:

    $ g8 n8han/giter8

This will create an sbt project with stub template sources nested
under `src/main/g8`. The file `default.properties` defines template
fields and their default values using the Java properties file format.
Every other file in that directory and below it is a template source.

[StringTemplate][st], wrapped by [Scalasti][scalasti], is the engine
for giter8 templates, so template fields in source files are bracketed
with the `$` character. For example, a "classname" field might be
referenced in the source as:

    class $classname$ {

[scalasti]: http://bmc.github.com/scalasti/
[st]: http://www.stringtemplate.org/

The "name" field, if defined, is treated specially by giter8. It is
assumed to be the name of a project being created, so the g8 runtime
creates a directory based off that name (with spaces and capitals
replaced) that will contain the template output. If no name field is
specified in the template, g8's output goes to the user's current
working directory. In both cases, directories nested under the
template's source directory are reproduced in its output. File and
directory names also participate in template expansion, e.g.

    src/main/g8/src/main/scala/$classname$.scala

If you enter sbt's interactive mode in the base directory of a
template project, the action "sbt-test" will apply the template in the
default output directory (under `target/g8`) and run `sbt update
test` for *that* project in a forked process. This is a good sanity
check for templates that are supposed to produce sbt projects.

But what if your template is not for an sbt project? Such as:

    src/main/g8/default.properties
    src/main/g8/TodaysMenu.html

You can still use sbt's interactive mode to test the template. The
lower level `write-templates` action will apply default field values
to the template and write it to the same `target/g8` directory.

As soon as you push your template to github (be sure to name the
project with a ".g8" extension) you can test it with the actual g8
runtime. When you're ready, add your template project to the
[the wiki][wiki] so other giter8 users can find it.

Question(s) that will probably be frequent
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


