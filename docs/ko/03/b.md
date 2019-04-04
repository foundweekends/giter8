---
out: testing.html
---

### 로컬환경에서 템플릿 테스트

`g8`명령에 `file://`을 사용하여 템플릿을 전달할 수있으므로, 템플릿을 로컬 파일 시스템에 둘 수있습니다.
 기존의 파일이 있어도 덮어 쓰기 `--force`옵션을 함께 사용하여 템플릿을 변경하면서 테스트 할 수있습니다.

예를들면, Unfiltered 템플릿을 로컬에 clone 했다고 할때, 다음과 같은 명령을 실행 할 수 있습니다.

    \$ g8 file://unfiltered.g8/ --name=uftest --force

터미널의 다른 창으로 이 템플릿을 테스트 해봅니다.

    \$ cd uftest/
    \$ sbt
    > ~ compile

템플릿을 변경하려면, `.g8` 디렉토리 아래에 파일을 저장하고, 처음 사용한 터미널에서, 아래 명령을 다시 실행 합니다.

    \$ g8 file://unfiltered.g8/ --name=uftest --force

`uftest` 의 sbt 세션은 `~ compile` 명령을 대기하고 있기 때문에, 변경을 감지하여 자동으로 재 컴파일이 일어납니다.

### Giter8Plugin 을 사용

Giter8는, 템플릿의 테스트를 위한 sbt 플러그인을 제공하고, 이것을 사용하면,
Github 브랜치에 템플릿을 push 하기전에 테스트를 해볼 수 있습니다.
위의 가이드대로, `foundweekends/giter8.g8` 템플릿을 사용하고 있다면 이미 구성되어 있습니다.

이미 사용하고 있는 템플릿을 현재 플러그인으로 업그레이드 하기위해서는 
`project/giter8.sbt` 에 아래 내용을 추가합니다.

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "$version$")
```

이 플러그인이 설정된 템플릿 기반 디렉토리에서 sbt 쉘에 들어가면 `g8Test` 라는 액션에서 
기본 출력 디렉토리 (`target/sbt-test`)에 템플릿이 적용되어 해당 프로젝트에 대해 [scriptted test][scripted] 를 fork된 프로세스에서 실행 한다. 
테스트 스크립트는`project/giter8.test`또는 `src/test/g8/test`로 제공 할 수있습니다. 만약 없으면 `>test`가 사용됩니다.
특히 sbt 프로젝트를 생성하는 템플릿은 이 방법으로 테스트 할 수 있을 것입니다.

그럼 sbt 프로젝트 이외의 템플릿은 어떻게 대응 할수 있을까요?

    project/default.properties
    TodaysMenu.html

그래도 sbt 쉘을 사용하여 템플릿을 테스트하는 것이 가능합니다.
`g8`라는 액션을 사용하면 기본 필드 값을 템플릿에 적용하여 `target/g8`디렉토리에 파일을 생성합니다.

Github에 템플릿을 push 하면 즉시 실제  런타임을 사용하여 테스트를하는 것이 가능합니다. 
(프로젝트 이름에 `.g8` 확장자를 잊지 않도록) 완성되면 템플릿 프로젝트 [wiki][wiki] 에 추가하여
다른 Giter8 사용자가 발견되도록 하여야 합니다.

  [scripted]: https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
  [wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates
