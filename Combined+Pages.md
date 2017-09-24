
Giter8
======

Giter8 is a command line tool to generate files and directories from
templates published on Github or any other git repository.
It's implemented in Scala and runs through the
[sbt launcher][launcher], but it can produce
output for any purpose.

### sbt new integration

Starting sbt 0.13.13, Giter8 can be called from sbt's ["new" command][new] as follows:

```
$ sbt new eed3si9n/hello.g8
```

### Credits

- Original implementation (C) 2010-2015 Nathan Hamblen and contributors
- Adapted and extended in 2016 by foundweekends project

Giter8 is licensed under Apache 2.0 license

[launcher]: http://www.scala-sbt.org/0.13/docs/Setup.html
[new]: http://www.scala-sbt.org/0.13/docs/sbt-new-and-Templates.html


Setup
-----

You can install Giter8 and other Scala command line tools with
[Conscript][cs]. This will setup Conscript in `~/.conscript/bin/cs`:

    curl https://raw.githubusercontent.com/foundweekends/conscript/master/setup.sh | sh

(See [Conscript's readme][cs] for a non-unixy option.) Once `cs` is
on your path, you can install (or upgrade) giter8 with this command:

    cs foundweekends/giter8

[cs]: http://www.foundweekends.org/conscript/setup.html

To make sure everything is working, try running `g8` with no
parameters. This should download giter8 and its dependencies, then print
a usage message.

When it's time to upgrade to a new version of giter8, just run the
same `cs` command again.

Giter8 is also installable with the OS X package manager [Homebrew][]:

    $ brew update && brew install giter8

[Homebrew]: http://brew.sh


Usage
-----

Template repositories must reside on github and be named with the
suffix `.g8`. We're keeping a [list of templates on the wiki][wiki].

To apply a template, for example, [unfiltered/unfiltered.g8][uft]:

[uft]: http://github.com/unfiltered/unfiltered.g8
[wiki]: http://github.com/foundweekends/giter8/wiki/giter8-templates

    $ g8 unfiltered/unfiltered.g8

Giter8 resolves this to the `unfiltered/unfiltered.g8`
repository and queries Github for the project's template
parameters.
Alternatively, you can also use a git repository full name

    $ g8 https://github.com/unfiltered/unfiltered.g8.git

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

    $ g8 unfiltered/unfiltered.g8 --name=my-new-website

Any unsupplied parameters are assigned their default values.

### Private Repositories

Giter8 will use your ssh key to access private repositories, just like git does.


  [CC0]: https://creativecommons.org/publicdomain/zero/1.0/

Making your own templates
-------------------------

### Use CC0 1.0 for template licensing

We recommend licensing software templates under [CC0 1.0][CC0],
which waives all copyrights and related rights, similar to the "public domain."

If you reside in a country covered by the Berne Convention, such as the US,
copyright will arise automatically without registration.
Thus, people won't have legal right to use your template if you do not
declare the terms of license.
The tricky thing is that even permissive licenses such as MIT License and Apache License will
require attribution to your template in the template user's software.
To remove all claims to the templated snippets, distribute it under CC0, which is an international equivalent to public domain.

```
Template license
----------------
Written in <YEAR> by <AUTHOR NAME> <AUTHOR E-MAIL ADDRESS>
[other author/contributor lines as appropriate]

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <http://creativecommons.org/publicdomain/zero/1.0/>.
```

### template layout

The g8 runtime looks for templates in two locations in a given Github project:

- If the `src/main/g8` directory is present it uses `src/main/g8` (`src` layout)
- If it does not exist, then the root directory is used (root layout)

### src layout

This src layout is recommended so that it is easy for the template
itself to be an sbt project. That way,
an sbt plugin can be employed to locally test templates before pushing
changes to github.

The easy way to start a new template project is with a Giter8 template
made expressly for that purpose:

    $ g8 foundweekends/giter8.g8

This will create an sbt project with stub template sources nested
under `src/main/g8`. The file `default.properties` defines template
fields and their default values using the Java properties file format.

### default.properties

`default.properties` file may be placed in `project/` directory,
or directly under the root of the template.
Properties are simple keys and values that replace them.

[StringTemplate][st], wrapped by [Scalasti][scalasti], is the engine
that applies Giter8 templates, so template fields in source files are
bracketed with the `$` character. For example, a "classname" field
might be referenced in the source as:

    class $classname$ {

[scalasti]: http://bmc.github.com/scalasti/
[st]: http://www.stringtemplate.org/

The template fields themselves can be utilized to define the defaults
of other fields.  For instance, you could build some URLs given the
user's Github id:

```
name = URL Builder
github_id=githubber
developer_url=https://github.com/$github_id$
project_url=https://github.com/$github_id$/$name;format="norm"$
```

This would yield the following in interactive mode:

```
name [URL Builder]: my-proj
github_id [githubber]: n8han
project_url [https://github.com/n8han/my-proj]:
developer_url [https://github.com/n8han]:
```

### Conditionals

All fields have a property named `truthy` to be used in [conditional expressions][conditionals].
`"y"`, `"yes"`, and `"true"` evaluate to `true`; anything else evaluates to `false`.

```
scala212 = yes
scala211 = no
```

These could be used in a template as follows:

<pre>
$if(scala212.truthy)$
scalaVersion := "2.12.3"
$elseif(scala211.truthy)$
scalaVersion := "2.11.11"
$else$
scalaVersion := "2.10.6"
$endif$
</pre>

[conditionals]: https://github.com/antlr/stringtemplate4/blob/master/doc/templates.md#conditionals

### name field

The `name` field, if defined, is treated specially by Giter8. It is
assumed to be the name of a project being created, so the g8 runtime
creates a directory based off that name (with spaces and capitals
replaced) that will contain the template output. If no name field is
specified in the template, `g8`'s output goes to the user's current
working directory. In both cases, directories nested under the
template's source directory are reproduced in its output. File and
directory names also participate in template expansion, e.g.

    src/main/g8/src/main/scala/$classname$.scala

### package field

The `package` field, if defined, is assumed to be the package name
of the user's source. A directory named `$package$` expands out to
package directory structure. For example, `net.databinder` becomes
`net/databinder`.

### verbatim field

The `verbatim` field, if defined, is assumed to be the space delimited
list of file patterns such as `*.html *.js`. Files matching `verbatim`
pattern are excluded from string template processing.

### Maven properties

*maven properties* tell Giter8 to query the Central Maven Repository.
Instead of supplying a particular version (and having to update
the template with every release), specify a library and giter8 will
set the value to the latest version according to Maven Central.

The property value format is `maven(groupId, artifactId)`.
Keep in mind that Scala projects are typically published with a
Scala version identifier in the artifact id. So for the Unfiltered
library, we could refer to the latest version as follows:

```
name = My Template Project
description = Creates a giter8 project template.
unfiltered_version = maven(ws.unfiltered, unfiltered_2.11)
```

To only use the latest stable release (excluding Milestone builds,
Release candidates etc) specify a "stable" value in the
property value format `maven(groupId, artifactId, stable)`.
To use the latest stable version for the Scalatest library
we could refer to it as follows:

```
name = My Template Project
description = Creates a giter8 project template.
scalatest_version = maven(org.scalatest, scalatest_2.11, stable)
```

### root layout

There's an experimental layout called root layout,
which uses the root directory of the Github project as
the root of template.

Since you can no longer include template fields in the files
under `project` its application is very limited.
It might be useful for templates that are not for sbt builds
or templates without any fields.


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

For file and directory names a format option can be specified after a double
underscore. For example, a directory named `$organization__packaged$` will
change `org.somewhere` to `org/somewhere` like the built-in support for
`package`. A file named `$name__Camel$.scala` and the name `awesome project`
will create the file `AwesomeProject.scala`.


### Testing templates locally

Templates may be passed to the `g8` command with a `file://` URL, and
in this case the template is applied as it is currently saved to the
file system. In conjunction with the `--force` option
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

### Using the Giter8Plugin

Giter8 supplies an sbt plugin for testing templates before pushing
them to a Github branch. If you used the `foundweekends/giter8.g8` template
recommended above, it should already be configured.


If you need to upgrade an existing template project to the current plugin, you can
add it as a source dependency in `project/giter8.sbt`:

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "0.9.0")
```

When you enter sbt's shell in the base directory of a
template project that is configured to use this plugin, the action
`g8Test` will apply the template in the default output directory
(under `target/sbt-test`) and run the [scripted test][scripted]
for *that* project in a forked process.  You can supply the test scripted as
`project/giter8.test` or `src/test/g8/test`, otherwise `>test` is used.
This is a good sanity check for templates that are supposed to produce sbt projects.

But what if your template is not for an sbt project?

    project/default.properties
    TodaysMenu.html

You can still use sbt's shell to test the template. The
lower level `g8` action will apply default field values
to the template and write it to the same `target/g8` directory.

As soon as you push your template to Github (remember to name the
project with a `.g8` extension) you can test it with the actual g8
runtime. When you're ready, add your template project to the
[the wiki][wiki] so other giter8 users can find it.

  [scripted]: http://www.scala-sbt.org/0.13/docs/Testing-sbt-plugins.html
  [wiki]: http://github.com/foundweekends/giter8/wiki/giter8-templates


Scaffolding plugin
------------------

Giter8 supplies an sbt plugin for creating and using scaffolds.

### Using the scaffold plugin

Add the following lines in `project/scaffold.sbt`

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.9.0")
```

Once done, the  `g8Scaffold` command can be used in the sbt shell.
Use TAB completion to discover available templates.

```
> g8Scaffold <TAB>
controller   global       model
```

The template plugin will prompt each property that needed to complete the scaffolding process:

```
> g8Scaffold controller
className [Application]:
```


### Creating a scaffold

The g8 runtime looks for scaffolds in the `src/main/scaffolds` in the given Github project.
Each directory inside `src/main/scaffolds` is a different scaffold, and will be
accessible in the sbt shell using the directory name. Scaffold directories
may have a `default.properties` file to define field values, just like
ordinary templates. `name` is again a special field name: if it exists,
the scaffold will be generated into a directory based on `name`,
with subdirectories following the layout of the source scaffold directory.

Once a template as been used, scaffolds are stored into `<project_root>/.g8`

```
$ ls sample/.g8
total 0
drwxr-xr-x   5 jtournay  staff   170B Aug  6 03:21 .
drwxr-xr-x  11 jtournay  staff   374B Aug  6 05:29 ..
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 controller
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 global
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 model
```

It's also possible to create your own scaffold in any sbt project by creating the `.g8` directory.


Contributing
-----

### Installing local version of giter8

When you're working on giter8 locally you probably want 
to try out your changes before you open a pull request. This is how you do it.

Giter8 uses [conscript] as distribution mechanism. You can find more documentation
about conscript on its [official page].

#### Fixing `PATH`:

Before you install giter8 with conscript, you need to ensure, that
conscript directory has higher precedence than default installation path.

You can either delete existing version of giter8, or change `PATH` variable such that 
`~/.conscript/bin` is before.

#### To install local version:

- Change `g8version` in `build.sbt` i.e. by adding `"-SNAPSHOT"`;
- Run `publishLocal` from sbt;
- From a shell session run `cs --local foundweekends/giter8/<YOUR_VERSION>`. 
  Use the version number you just wrote in `build.sbt`.

#### To refresh:

- Run `publishLocal` from sbt again;
- From a shell session run `cs --clean-boot`.

#### To get back to normal version:

From a shell session run `cs foundweekends/giter8`.

[official page]: https://github.com/foundweekends/conscript
[conscript]: http://www.foundweekends.org/conscript/
