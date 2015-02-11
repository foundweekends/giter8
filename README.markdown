giter8
======

[*Japanese 日本語*](http://blog.twiwt.org/e/f12c0f)

Giter8 is a command line tool to generate files and directories from
templates published on github or any other git repository. It's 
implemented in Scala and runs through the 
[Simple Build Tool launcher][launcher], but it can produce 
output for any purpose.

[launcher]: https://github.com/harrah/xsbt/wiki/

Installation
------------

You can install giter8 and other Scala command line tools with
[Conscript][cs]. This will setup Conscript in `~/bin/cs`:

    curl https://raw.githubusercontent.com/n8han/conscript/master/setup.sh | sh

(See [Conscript's readme][cs] for a non-unixy option.) Once `cs` is
on your path, you can install (or upgrade) giter8 with this command:

    cs n8han/giter8

[cs]: https://github.com/n8han/conscript#readme

To make sure everything is working, try running `g8` with no
parameters. This should download giter8 and its dependencies, then print
a usage message.

When it's time to upgrade to a new version of giter8, just run the
same `cs` command again.

Giter8 is also installable with the OS X package manager [Homebrew][]:

    $ brew update && brew install giter8

[Homebrew]: http://mxcl.github.com/homebrew/

Usage
-----

Template repositories must reside on github and be named with the
suffix `.g8`. We're keeping a [list of templates on the wiki][wiki].

To apply a template, for example, [softprops/unfiltered.g8][uft]:

[uft]: http://github.com/softprops/unfiltered.g8
[wiki]: http://github.com/n8han/giter8/wiki/giter8-templates

    $ g8 softprops/unfiltered.g8

The `.g8` suffix is assumed:

    $ g8 softprops/unfiltered

Either way, giter8 resolves this to the `softprops/unfiltered.g8`
repository and queries github for the project's template
parameters. 
Alternatively, you can also use a git repository full name>

	$ g8 https://github.com/softprops/unfiltered.g8.git

You'll be prompted for each parameter, with its default
value in square brackets:

    name [My Web Project]: 

Enter your own value or press enter to accept the default. After all
values have been supplied, giter8 fetches the templates, applies
the parameters, and writes them to your filesystem. 

If the template has a `name` parameter, it will be used to create base 
directory in the current directory (typical for a new project). 
Otherwise, giter8 will output its files and directories into 
the current directory, skipping over any files that already exist.

Once you become familiar with a template's parameters, you can enter
them on the command line and skip the interaction:

    $ g8 softprops/unfiltered.g8 --name=my-new-website

Any unsupplied parameters are assigned their default values.

Private Repositories
--------------------

Giter8 will use your ssh key to access private repositories, just like git does.

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

Properties can be simple keys and values that replace them, but **ls
properties** tell giter8 to query the [ls.implicit.ly][ls] web
API. Instead of supplying a particular version (and having to update
the template with every release), specify a library and giter8 will
set the value to the latest version according to ls.

[ls]: http://ls.implicit.ly/

The property value format is `ls(library, user, repo)`. The second two
parameters are optional, but it is a good idea to specify the github
at least the user or organization that is bound to the library, in
case someone else ever publishing a library module with the same name.

The n8han/giter8.g8 template itself uses this feature to refer
to the latest version of the giter8 sbt plugin.

    name = My Template Project
    description = Creates a giter8 project template.
    giter8_version = ls(giter8-plugin, n8han)

Giter8 can also resolve the latest version of projects through the
**Central Maven Repository**.

In your `default.properties`, define a value as `maven(groupId,
artifactId)` to have its latest version looked up from the central
repo. Keep in mind that Scala projects are typically published with a
Scala version identifier in the artifact id. So for the Unfiltered
library, we could refer to the latest version as follows:

    unfiltered_version = maven(net.databinder, unfiltered_2.11)

[StringTemplate][st], wrapped by [Scalasti][scalasti], is the engine
that applies giter8 templates, so template fields in source files are
bracketed with the `$` character. For example, a "classname" field
might be referenced in the source as:

    class $classname$ {

[scalasti]: http://bmc.github.com/scalasti/
[st]: http://www.stringtemplate.org/

The template fields themselves can be utilized to define the defaults
of other fields.  For instance, you could build some URLs given the
user's github id:

    name = URL Builder
    github_id=githubber
    developer_url=https://github.com/$github_id$
    project_url=https://github.com/$github_id$/$name;format="norm"$
    
This would yield the following in interactive mode:

    name [URL Builder]: my-proj
    github_id [githubber]: n8han
    project_url [https://github.com/n8han/my-proj]:
    developer_url [https://github.com/n8han]:

The `name` field, if defined, is treated specially by giter8. It is
assumed to be the name of a project being created, so the g8 runtime
creates a directory based off that name (with spaces and capitals
replaced) that will contain the template output. If no name field is
specified in the template, g8's output goes to the user's current
working directory. In both cases, directories nested under the
template's source directory are reproduced in its output. File and
directory names also participate in template expansion, e.g.

    src/main/g8/src/main/scala/$classname$.scala
    
The `package` field, if defined, is assumed to be the package name
of the user's source. A directory named `$package$` expands out to
package directory structure. For example, `net.databinder` becomes
`net/databinder`.

The `verbatim` field, if defined, is assumed to be the space delimited
list of file patterns such as `*.html *.js`. Files matching `verbatim`
pattern are excluded from string template processing.

### Formatting template fields

Giter8 has built-in support for formatting template fields. Formatting options
can be added when referencing fields. For example, the `name` field can be
formatted in upper camel case with:

    $name;format="Camel"$

The formatting options are:

    upper    | uppercase       : all uppercase letters
    lower    | lowercase       : all lowercase letters
    cap      | capitalize      : uppercase first letter
    decap    | decapitalize    : lowercase first letter
    start    | start-case      : uppercase the first letter of each word
    word     | word-only       : remove all non-word letters (only a-zA-Z0-9_)
    Camel    | upper-camel     : upper camel case (start-case, word-only)
    camel    | lower-camel     : lower camel case (start-case, word-only, decapitalize)
    hyphen   | hyphenate       : replace spaces with hyphens
    norm     | normalize       : all lowercase with hyphens (lowercase, hyphenate)
    snake    | snake-case      : replace spaces and dots with underscores
    packaged | package-dir     : replace dots with slashes (net.databinder -> net/databinder)
    random   | generate-random : appends random characters to the given string

A `name` field with a value of `My Project` could be rendered in several ways:

    $name$ -> "My Project"
    $name;format="camel"$ -> "myProject"
    $name;format="Camel"$ -> "MyProject"
    $name;format="normalize"$ -> "my-project"
    $name;format="lower,hyphen"$ -> "my-project"

Note that multiple format options can be specified (comma-separated) which will
be applied in the order given.

For file and directory names formatting options can be specified after a double
underscore. For example, a directory named `$organization__packaged$` will
change `org.somewhere` to `org/somewhere` like the built-in support for
`package`. A file named `$name__Camel$.scala` and the name `awesome project`
will create the file `AwesomeProject.scala`. Multiple formatting options can
be specified by separating them with an underscore. For example,
`$name__word_lower$.scala` and the name `AWESOME !!` will create the file
`awesome.scala`.

### Testing templates locally

Templates may be passed to the `g8` command with a `file://` URL, and
in this case the template is applied as it is currently saved to the
file system (giter8 0.6.0 +). In conjunction with the `--force` option
which overwrites output files without prompting, you can test changes
to a template as you are making them.

For example, if you have the Unfiltered template cloned locally you
could run a command like this:

    $ g8 file://unfiltered.g8/ --name=uftest --force

In a separate terminal, test out the template.

    $ cd uftest/
    $ sbt
    > ~ compile

To make changes to the template, save them to its source under the
`.g8` directory, then repeat the command to apply the template in the
original terminal:

    $ g8 file://unfiltered.g8/ --name=uftest --force

Your `uftest` sbt session, waiting with the `~ compile` command, will
detect the changes and automatically recompile.

### Using the giter8-plugin

Giter8 supplies an sbt plugin for testing templates before pushing
them to a github branch. If you used the `n8han/giter8.g8` template
recommended above, it should already be configured. If you need to
upgrade an existing template project to the current plugin, you can
add it as a source dependency in `project/project/plugins.scala`:

```scala
import sbt._
object PluginDef extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn( g8plugin )
  lazy val g8plugin =
    ProjectRef(uri("git://github.com/n8han/giter8#0.4.4"), "giter8-plugin")
}
```

And settings must be applied in a `build.sbt` file in the project base:

    seq(giter8Settings :_*)

When you enter sbt's interactive mode in the base directory of a
template project that is configured to use this plugin, the action
`g8-test` will apply the template in the default output directory
(under `target/sbt-test`) and run the scripted test for *that* project
in a forked process.  You can supply the test scripted as
`src/test/g8/test`, otherwise `>test` is used. This is a good sanity
check for templates that are supposed to produce sbt projects.

But what if your template is not for an sbt project?

    src/main/g8/default.properties
    src/main/g8/TodaysMenu.html

You can still use sbt's interactive mode to test the template. The
lower level `g8` action will apply default field values
to the template and write it to the same `target/g8` directory.

As soon as you push your template to github (remember to name the
project with a `.g8` extension) you can test it with the actual g8
runtime. When you're ready, add your template project to the
[the wiki][wiki] so other giter8 users can find it.


## Scaffolding plugin

Giter8 supplies an sbt plugin for creating and using scaffolds.

## Using the scaffold plugin

Add the following lines in `project/plugins.sbt`

```scala
addSbtPlugin("net.databinder.giter8" % "giter8-scaffold" % "0.4.6-SNAPSHOT")
```

You also want to add `giter8.ScaffoldPlugin.scaffoldSettings` to you project.

```scala
lazy val project = Project("project").settings(giter8.ScaffoldPlugin.scaffoldSettings:_*)
```

Once done, the  `g8-scaffold` command can be used in the sbt console.
Use TAB completion to discover available templates.

```
[sample] $ g8-scaffold <TAB>
controller   global       model
```

The template plugin will prompt each property that needed to complete the scaffolding process:

```
[sample] $ g8-scaffold controller
className [Application]:
```


## creating a scaffold

The g8 runtime looks for scaffold in the `src/main/scaffolds`.
Each folder inside ``src/main/scaffolds` is a different scaffold, and will be accessible in the sbt console using the folder name. 

Once a template as been used, scaffolds are stored into `<project_root>/.g8`

```
➜  sample/.g8 
total 0
drwxr-xr-x   5 jtournay  staff   170B Aug  6 03:21 .
drwxr-xr-x  11 jtournay  staff   374B Aug  6 05:29 ..
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 controller
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 global
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 model
```

It's also possible to create your own scaffold in any sbt project by creating or modifying the `.g8` folder.



