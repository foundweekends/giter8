
Giter8
======

Giter8 is a command line tool to generate files and directories from
templates published on Github or any other git repository.
It's implemented in Scala and runs through the 
[sbt launcher][launcher], but it can produce 
output for any purpose.

### Credits

- Original implementation (C) 2010-2015 Nathan Hamblen and contributors
- Adapted and extended in 2016 by foundweekends project

Giter8 is licensed under Apache 2.0 license

[launcher]: http://www.scala-sbt.org/0.13/docs/Setup.html


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

[Homebrew]: http://mxcl.github.com/homebrew/


Usage
-----

Template repositories must reside on github and be named with the
suffix `.g8`. We're keeping a [list of templates on the wiki][wiki].

To apply a template, for example, [softprops/unfiltered.g8][uft]:

[uft]: http://github.com/softprops/unfiltered.g8
[wiki]: http://github.com/foundweekends/giter8/wiki/giter8-templates

    $ g8 softprops/unfiltered.g8

Giter8 resolves this to the `softprops/unfiltered.g8`
repository and queries Github for the project's template
parameters.
Alternatively, you can also use a git repository full name

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

### Private Repositories

Giter8 will use your ssh key to access private repositories, just like git does.


Making your own templates
-------------------------

The Giter8 runtime looks for templates in two locations in a given Github project:

- If the `src/main/g8` directory is present it uses `src/main/g8` (`src` layout)
- If it does not exist, then the root directry is used (**root layout**)

### root layout

For new templates, the root layout is recommended.
The easy way to start a new template project is with a giter8 template
made expressly for that purpose:

    $ g8 foundweekends/giter8.g8

This will create an sbt project with stub template sources.
The file `project/default.properties` defines template
fields and their default values using the Java properties file format.

### src layout

Older templates embed a build inside the `src/main/g8` directory,
and places `default.properties` under it too.

### default.properties

`default.properties` file may be placed in `project/` directory,
or directly under the root of the tempalate.
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
unfiltered_version = maven(net.databinder, unfiltered_2.11)
```
