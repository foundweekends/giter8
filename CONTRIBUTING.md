
## Deployment

Commit version.

```
git tag -s v1.x.x -m "1.x.x"
jenv shell 1.6
sbt
> clean
> ++2.10.6
> library/publishSigned
> app/publishSigned
> plugin/publishSigned
> scaffold/publishSigned
> ++2.11.8
> library/publishSigned
> app/publishSigned
> exit
jenv shell 1.8
sbt
> ++2.12.1
> library/publishSigned
> app/publishSigned
```

