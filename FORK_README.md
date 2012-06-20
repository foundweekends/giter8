# jto/giter8

This is a fork of the original giter8 project (https://github.com/n8han/giter8).

## Building and running

just run `sbt` in the source folder. `sbt` should download dependencies, and give you a prompt.

```
➜  giter8 git:(master) sbt
[info] Loading project definition from /Users/jtournay/Documents/giter8/project
[info] Set current project to giter8 (in build file:~/Documents/giter8/)
> 
```

Use the `app` project:

```
> project app
[info] Set current project to giter8 (in build file:~/Documents/giter8/)
```

You can now run g8 using the `run` command:

```
> run typesafehub/play-scala
```

This fork adds support of any `git` repository, using its url:

```
> run https://github.com/typesafehub/play-scala.g8.git
```




