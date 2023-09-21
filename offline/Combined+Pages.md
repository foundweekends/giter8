
Giter8
======

Giter8 is a command line tool to generate files and directories from
templates published on GitHub or any other git repository.
It's implemented in Scala and runs through the
[sbt launcher][launcher], but it can produce
output for any purpose.

### sbt new integration

Starting sbt 0.13.13, Giter8 can be called from sbt's ["new" command][new] as follows:

```
$ sbt new scala/scala-seed.g8
```

### Credits

- Original implementation (C) 2010-2015 Nathan Hamblen and contributors
- Adapted and extended in 2016 by foundweekends project

Giter8 is licensed under Apache 2.0 license

[launcher]: https://www.scala-sbt.org/1.x/docs/Setup.html
[new]: https://www.scala-sbt.org/1.x/docs/sbt-new-and-Templates.html


Setup
-----

#### Coursier

Giter8 and other Scala command line tools can be installed using [Coursier](https://get-coursier.io/). 
See the coursier [installation instruction](https://get-coursier.io/docs/cli-installation) to add it to your path.
Once `cs` is on your path, you can install giter8 with this command:

    $ cs install giter8

and update it using:

    $ cs update g8

#### Manual

It's possible to manually download and install giter8 directly from Maven Central:

    $ curl https://repo1.maven.org/maven2/org/foundweekends/giter8/giter8-bootstrap_2.12/0.16.2/giter8-bootstrap_2.12-0.16.2.sh > ~/bin/g8
    $ chmod +x ~/bin/g8

Replace `~/bin/` with anything that is on your `PATH`. To make sure everything is working, try running `g8` with no
parameters, you should see

    Error: Missing argument <template>
    Try --help for more information.


Usage
-----

Template repositories can reside on GitHub and should be named with the
suffix `.g8`. We're keeping a [list of templates on the wiki][wiki].

To apply a template, for example, [unfiltered/unfiltered.g8][uft]:

[uft]: https://github.com/unfiltered/unfiltered.g8
[wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates

    $ g8 unfiltered/unfiltered.g8

Giter8 resolves this to the `unfiltered/unfiltered.g8`
repository and queries GitHub for the project's template
parameters.
Alternatively, you can also use a git repository full name

    $ g8 https://gitlab.com/unfiltered/unfiltered-gitlab.g8.git

or even a local template, using the `file://` protocol:

    $ g8 file://path/to/template

For remote or local repositories it's possible to fetch a specific branch,
a specific tag or even a specific directory using command-line arguments:

    -b, --branch <value>     Resolve a template within a given branch
    -t, --tag <value>        Resolve a template within a given tag
    -d, --directory <value>  Resolve a template within the given 
                             subdirectory in the repo

 The default enclosing directory is `.`.

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

An output directory can be specified:

    -o, --out <value>        Output directory

this will override the generation of the directory's name according to the value
of the `name` variable and the current directory as the enclosing one.

To overwrite existing files in the destination folder, you can use:

    -f, --force              Force overwrite of any existing files in 
                             output directory

Once you become familiar with a template's parameters, you can enter
them on the command line and skip the interaction:

    $ g8 unfiltered/unfiltered.g8 --name=my-new-website

Any unsupplied parameters are assigned their default values.

### Private Repositories

Giter8 will use your ssh key to access private repositories, just like git does.

### SSH Agent

Giter8 now support proxying to an SSH Agent which can be useful if you are using another SSH agent
such as `gpg-agent`.

Consider the following example:

`~/.gitconfig`:

    [url "ssh://git@github.com"]
        insteadOf = https://github.com

`~/.profile`:

    export SSH_AUTH_SOCK="$(gpgconf --list-dirs agent-ssh-socket)"
    gpgconf --launch gpg-agent 

Then this would have previously failed with:

    $ g8 unfiltered/unfiltered.g8
    
    ssh://git@github.com/unfiltered/unfiltered.g8.git: Auth fail

This now works provided that the GitHub public key is in your known hosts file.

You can do this by running:

    $ ssh -T git@github.com

Optionally the known hosts file can be overridden using:

    -h, --known-hosts <value>  SSH known hosts file. If unset the location 
                               will be guessed.


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
This template is distributed without any warranty. See <https://creativecommons.org/publicdomain/zero/1.0/>.
```

### template layout

The g8 runtime looks for templates in two locations in a given GitHub project:

- If the `src/main/g8` directory is present it uses `src/main/g8` (`src` layout)
- If it does not exist, then the root directory is used (root layout)

### src layout

This src layout is recommended so that it is easy for the template
itself to be an sbt project. That way,
an sbt plugin can be employed to locally test templates before pushing
changes to GitHub.

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

[StringTemplate][st] is the engine
that applies Giter8 templates, so template fields in source files are
bracketed with the `$` character. For example, a "classname" field
might be referenced in the source as:

    class $classname$ {

[st]: https://www.stringtemplate.org/

The template fields themselves can be utilized to define the defaults
of other fields.  For instance, you could build some URLs given the
user's GitHub id:

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

Dollar signs can be escaped to avoid resolution:

```
val foo = "foo"
val bar = "bar"
println(s"\$foo\$bar")
```

This would yield to:

```
val foo = "foo"
val bar = "bar"
println(s"$foo$bar")
```

Some variable names are prohibited since they're tokens used by [StringTemplate][st]
in its grammar, the complete list is [here](https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md) but the most common are:

```
"i", "i0", "if", "else", "elseif", "endif", "first", "length"
"strlen", "last", "rest", "reverse", "trunc", "strip", "trim"
```

### Template comments

Sometimes it's useful to put a comment into a template that is intended for 
template maintainers, and should not be included in the generated output.

Wrapping comments between `$!` and `!$` won't make them appear in the output.

```
$! This comment won't appear in the output !$
// This comment will appear in the output
$!
This multiline comment won't appear either
No matter how
long it is

Internal $substitutions$ are ignored.

Even $invalid$ ones.

!$
/*
 * This comment is output and can contain $substitutions$
 */
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


These could also be used include/exclude files or directories:

```bash
src/main/g8
├── $name__normalize$
│   ├── $if(jvm.truthy)$jvm$endif$
│   │   └── src
│   │       └── main
│   │           └── scala
│   │               └── $organization__packaged$
│   │                   └── $name;format="Camel"$.scala

```

If you want to skip a directory from the path, but keep all nested directories and files, use `.` as the name of the directory. For example the next template:

```
src/main/g8
├── parent_folder
│   ├── $if(cond.truthy)$skip_folder$else$.$endif$
|   |   └── child_file
```

will be processed to

```
├── parent_folder
|   └── child_file
```

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
which uses the root directory of the GitHub project as
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

    upper      | uppercase       : all uppercase letters
    lower      | lowercase       : all lowercase letters
    cap        | capitalize      : uppercase first letter
    decap      | decapitalize    : lowercase first letter
    start      | start-case      : uppercase the first letter of each word
    word       | word-only       : remove all non-word letters (only a-zA-Z0-9_)
    space      | word-space      : replace all non-word letters (only a-zA-Z0-9) with a whitespace
    Camel      | upper-camel     : upper camel case (start-case, word-only)
    camel      | lower-camel     : lower camel case (start-case, word-only, decapitalize)
    hyphen     | hyphenate       : replace spaces with hyphens
    norm       | normalize       : all lowercase with hyphens (lowercase, hyphenate)
    snake      | snake-case      : replace spaces and dots with underscores
    dotReverse | dot-reverse     : tokenizes by dot and reverses the tokens (scala-lang.org -> org.scala-lang)
    package    | package-naming  : replace spaces with dots
    packaged   | package-dir     : replace dots with slashes (net.databinder -> net/databinder)
    random     | generate-random : appends random characters to the given string

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
will create the file `AwesomeProject.scala`. Multiple comma separated formatting 
options can be used at once: `$name__lower,hyphen$.scala` and the name 
`Awesome Project` will create the file `awesome-project.scala`.


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
them to a GitHub branch. If you used the `foundweekends/giter8.g8` template
recommended above, it should already be configured.


If you need to upgrade an existing template project to the current plugin, you can
add it as a source dependency in `project/giter8.sbt`:

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "0.16.2")
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

As soon as you push your template to GitHub (remember to name the
project with a `.g8` extension) you can test it with the actual g8
runtime. When you're ready, add your template project to the
[the wiki][wiki] so other giter8 users can find it.

### Using Mill

There is also an external [Mill plugin][mill-plugin] that can be used to test
your templates as well. An example setup can be found below:

```scala
import $ivy.`io.chris-kipp::mill-giter8::0.2.0`

import io.kipp.mill.giter8.G8Module

object g8 extends G8Module {
  override def validationTargets =
    Seq("example.compile", "example.fix", "example.reformat")
}
```

This plugin only supports [`src` layouts][src-layout], but gives you some useful
targets like `g8.validate` which will both test the generation of your template
and also ensure any targets defined with `validationTargets` can also be ran
against your generated project.

  [scripted]: https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
  [wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates
  [mill-plugin]: https://github.com/ckipp01/mill-giter8
  [src-layout]: https://www.foundweekends.org/giter8/template.html#template+layout


Scaffolding plugin
------------------

Giter8 supplies an sbt plugin for creating and using scaffolds.

### Using the scaffold plugin

Add the following lines in `project/scaffold.sbt`

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.16.2")
```

Once done, the  `g8Scaffold` command can be used in the sbt shell.
Use TAB completion to discover available templates.

```
> g8Scaffold <TAB>
controller   global       model
```

To overwrite existing files pass the `--force` flag after the template:

```
> g8Scaffold model --force
```

The template plugin will prompt each property that needed to complete the scaffolding process:

```
> g8Scaffold controller
className [Application]:
```


### Creating a scaffold

The g8 runtime looks for scaffolds in the `src/main/scaffolds` in the given GitHub project.
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
[conscript]: https://www.foundweekends.org/conscript/
