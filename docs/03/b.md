---
out: testing.html
---

### Testing templates locally

Templates may be passed to the `g8` command with a `file://` URL, and
in this case the template is applied as it is currently saved to the
file system. In conjunction with the `--force` option
which overwrites output files without prompting, you can test changes
to a template as you are making them.

For example, if you have the Unfiltered template cloned locally you
could run a command like this:

    \$ g8 file://unfiltered.g8/ --name=uftest --force

In a separate terminal, test out the template.

    \$ cd uftest/
    \$ sbt
    > ~ compile

To make changes to the template, save them to its source under the
`.g8` directory, then repeat the command to apply the template in the
original terminal:

    \$ g8 file://unfiltered.g8/ --name=uftest --force

Your `uftest` sbt session, waiting with the `~ compile` command, will
detect the changes and automatically recompile.

### Using the Giter8Plugin

Giter8 supplies an sbt plugin for testing templates before pushing
them to a GitHub branch. If you used the `foundweekends/giter8.g8` template
recommended above, it should already be configured.


If you need to upgrade an existing template project to the current plugin, you can
add it as a source dependency in `project/giter8.sbt`:

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "$version$")
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

  [scripted]: https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
  [wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates
