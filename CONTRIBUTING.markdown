

```
jenv shell 1.6
sbt
> ++2.10.6
> clean
> compile
> publishSigned
> ++2.11.11
> lib/compile
> lib/publishSigned
> exit

jevn shell 1.8
sbt
> clean
> ++2.12.3
> lib/compile
> lib/publishSigned
```
