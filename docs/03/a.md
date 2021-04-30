---
out: formatting.html
---

### Formatting template fields

Giter8 has built-in support for formatting template fields. Formatting options
can be added when referencing fields. For example, the `name` field can be
formatted in upper camel case with:

    \$name;format="Camel"\$

The formatting options are:

    upper      | uppercase            : all uppercase letters
    lower      | lowercase            : all lowercase letters
    cap        | capitalize           : uppercase first letter
    decap      | decapitalize         : lowercase first letter
    start      | start-case           : uppercase the first letter of each word
    word       | word-only            : remove all non-word letters (only a-zA-Z0-9_)
    space      | word-space           : replace all non-word letters (only a-zA-Z0-9) with a whitespace
    Camel      | upper-camel          : upper camel case (start-case, word-only)
    camel      | lower-camel          : lower camel case (start-case, word-only, decapitalize)
    hyphen     | hyphenate            : replace spaces with hyphens
    norm       | normalize            : all lowercase with hyphens (lowercase, hyphenate)
    snake      | snake-case           : replace spaces and dots with underscores
    reverseOrg | reverse-organization : tokenizes by dot and reverses the tokens (scala-lang.org -> org.scala-lang)
    package    | package-naming       : replace spaces with dots
    packaged   | package-dir          : replace dots with slashes (net.databinder -> net/databinder)
    random     | generate-random      : appends random characters to the given string

A `name` field with a value of `My Project` could be rendered in several ways:

    \$name\$ -> "My Project"
    \$name;format="camel"\$ -> "myProject"
    \$name;format="Camel"\$ -> "MyProject"
    \$name;format="normalize"\$ -> "my-project"
    \$name;format="lower,hyphen"\$ -> "my-project"

Note that multiple format options can be specified (comma-separated) which will
be applied in the order given.

For file and directory names a format option can be specified after a double
underscore. For example, a directory named `\$organization__packaged\$` will
change `org.somewhere` to `org/somewhere` like the built-in support for
`package`. A file named `\$name__Camel\$.scala` and the name `awesome project`
will create the file `AwesomeProject.scala`.
