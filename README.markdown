Giter8
======

Giter8 is a command line tool to generate files and directories from
templates published on GitHub or any other Git repository. 
It’s implemented in Scala and runs through the [sbt launcher](https://www.scala-sbt.org/1.x/docs/Setup.html), but it can produce output for any purpose.

Setup
-------
**Coursier**

Giter8 and other Scala command line tools can be installed using Coursier. 
See the coursier installation instruction to add it to your path.
Once `cs` is on your path, you can install giter8 with this command:

```
$ cs install giter8
```
and update it using:
```
$ cs update g8
```
**Manual**

It’s possible to manually download and install giter8 directly from Maven Central:

```
$ curl https://repo1.maven.org/maven2/org/foundweekends/giter8/giter8-bootstrap_2.12/0.13.1/giter8-bootstrap_2.12-0.13.1.sh > ~/bin/g8
$ chmod +x ~/bin/g8
```
Replace `~/bin/` with anything that is on your `PATH`. To make sure everything is working, try running `g8` with no parameters, you should see
```
Error: Missing argument <template>
Try --help for more information. 
```

For more details see [Giter8 documentation][docs].



Credits
-------

- Original implementation (C) 2010-2015 Nathan Hamblen and contributors
- Adapted and extended in 2016 by foundweekends project

Giter8 is licensed under Apache 2.0 license

  [docs]: http://www.foundweekends.org/giter8/
