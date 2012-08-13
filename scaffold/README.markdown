g8-scaffold
======

g8-scaffold add code generation abilities to giter8, after a project has been generated.

Installation
------------

You need to add the plugin to your sbt project, in `project/plugins.sbt`

```scala
addSbtPlugin("net.databinder.giter8" % "giter8-scaffold" % "0.4.6-SNAPSHOT")
```

Usage
-----

giter8 expects scaffold templates to live into `src/main/scaffolds` in the original g8 template.

Scaffolding files will be copied into the `.g8` folder of your project.

Generating code
-----

In the sbt console type:

```scala
	g8-scaffold <scaffold_name>
```

The name of the scaffold is the name of the folder located directly under `.g8`

Assuming you `.g8` folder has the following structure:

```
	.g8
	 |_ model
	 |_ view
	 |_ controller
```

You have 3 different scaffodings available.

To generate a new template, just type `g8-scaffold model`. 
As usual, g8 will ask for the variable values, and generate the correct code.