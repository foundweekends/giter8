
Giter8
======

Giter8 는, GitHub을 비롯한 Git 저장소에 게시 된 템플릿 사용하여 파일 및 디렉토리를 생성하기위한 명령 줄 도구입니다.
Scala 를 사용하여 구현되어 있어, [sbt launcher][launcher] 를 사용하여 실행되지만, 그 출력은 어떤 용도로도 사용할 수 있습니다.

### sbt new 와의 통합

sbt 0.13.13 부터는 Giter8 는 sbt 의 ["new"][new] 명령으로 부터 호출할 수 있습니다.

```
$ sbt new scala/scala-seed.g8
```

### 공헌

- 최초구현 (C) 2010-2015 Nathan Hamblen 와 기타 기여자
- 2016년 이후 foundweekends 프로젝트를 통해 기여하고 있습니다.

Giter8 is licensed under Apache 2.0 license

[launcher]: https://www.scala-sbt.org/1.x/docs/Setup.html
[new]: https://www.scala-sbt.org/1.x/docs/sbt-new-and-Templates.html


설치
----------

Giter8및 기타 Scala 명령줄 도구는 [Conscript][cs]
를 사용하여 설치 할 수 있다. 다음의 방법으로 Conscript 을 `~/.conscript/bin/cs` 로 설치 합니다.

    curl https://raw.githubusercontent.com/foundweekends/conscript/master/setup.sh | sh

(다른 설치 방법도 있으므로, [Conscript 의 설치방법][cs]참고.)
PATH가 적용되어, `cs` 명령을 실행할 수 있는 위치에서、아래의 방법으로 Giter8 를 설치(또는 업그레이드)하는것이 가능합니다:

    cs foundweekends/giter8

[cs]: https://www.foundweekends.org/conscript/ja/setup.html

동작을 확인 하려면, `g8` 매개변수 없이 실행 합니다.。
Giter8 와 종속 라이브러리가 다운로드되어, 사용방법이 표시됩니다.

업그레이드 시에도 같은 `cs` 실행하면 됩니다.

Giter8 는 OS X 의 패키지 매니져인 [Homebrew][] 를 통해서도 설치가 가능 합니다.

    $ brew update && brew install giter8

[Homebrew]: https://brew.sh


사용 방법
-------

템플릿 리포지토리는 GitHub 상에서는 `.g8` 로 확장자가 끝나도록 규약을 만들었습니다.
[Wiki에 등록된 템플릿 목록][wiki] 를 통해 관리하고 있습니다.

예를들면 [unfiltered/unfiltered.g8][uft] 라는 템플릿을 적용하기 위해서는 다음을 실행합니다.

[uft]: https://github.com/unfiltered/unfiltered.g8
[wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates

    $ g8 unfiltered/unfiltered.g8

Giter8에서는 이것을 `unfiltered/unfiltered.g8` 라는 리포지토리로 인식하고, 프로젝트 템플릿 및 매개 변수를 받습니다. 
그 밖에, git 리포지토리 주소를 전체 이름으로 지정하는 것도 가능합니다.

    $ g8 https://github.com/unfiltered/unfiltered.g8.git

템플릿을 실행하면 각 매개 변수에 대한 프롬프트가 표시됩니다. 이 때 기본값은 대괄호로 표시됩니다. 

    name [My Web Project]: 

임의의 값을 기록하거나 엔터 키를 눌러 기본값을 그대로 사용합니다. 
모든 값이 전달되면 Giter8 템플릿을 불러와 입력한 매개 변수를 적용하여 파일 시스템에 쓰기를 합니다.

템플릿에는 `name`이라는 매개 변수가 있는데, 그것은 현재 디렉토리 아래의 기본 디렉토리 이름으로 사용되는 (새로운 프로젝트를 생성 할 때 일반적으로, 이 방법을 사용). 
`name`이 없다면 Giter8는 현재 디렉토리에 파일이나 디렉토리에 대한 쓰기를 수행하지만, 기존 파일이있는 경우는 생략됩니다.

템플릿 매개 변수에 익숙해지면 명령 줄에서 직접 지정하여 처리하는 것도 가능합니다.

    $ g8 unfiltered/unfiltered.g8 --name=my-new-website

이 때 지정되지 않은 매개 변수는 기본값이 할당되므로 주의 합니다.

### 개인 리포지토리

Giter8 는 Git과 마찬가지로, ssh 키를 사용하여, 개인 개인 리포지토리에 엑세스 할수 있습니다.


  [CC0]: https://creativecommons.org/publicdomain/zero/1.0/

템플릿 생성 방법
-----------------

### 템플릿 라이센스는 CC0 1.0 을 사용합니다.

[CC0 1.0][CC0]을 사용하는 것을 권장합니다. 소프트웨어의 템플릿 라이센스는 이른바 '공개'뿐만 아니라 모든 저작권 및 이로인해 발생되는 권리를 포기합니다.
미국 같은 베른 협약이 적용되는 나라에 거주하고 있다면, 저작권은 등록 없이 자동으로 발생합니다. 
따라서 라이센스 조건을 선언하지 않으면 사용자는 템플릿을 사용할 법적 권리가 없습니다.
까다로운 것은 MIT 라이선스와 Apache 라이선스와 같은 허용된 라이선스조차도 템플릿 사용자의 소프트웨어에서 당신의 템플릿에 귀속되어야 한다는 것이다.
템플릿의 스니펫에 대한 모든 클레임을 지우려면 공개 도메인과 동등한 국제 CC0에 배포하십시오.
```
Template license
----------------
Written in <YEAR> by <AUTHOR NAME> <AUTHOR E-MAIL ADDRESS>
[other author/contributor lines as appropriate]

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <https://creativecommons.org/publicdomain/zero/1.0/>.
```

### 템플릿의 레이아웃

GitHub 프로젝트가 있을때, Giter8 런타임은 다음의 두가지 경로로 템플릿을 찾아 이동합니다
- 만약 `src/main/g8` 디렉토리가 있으면, `src/main/g8` 을 사용됩니다. (`src` 레이아웃)
- 만약 없으면, 상위디렉토리가 그대로 사용됩니다. (root 레이아웃)

### src 레이아웃
템플릿 자체가 sbt 프로젝트인 것이 쉽기 때문에 기본적으로 src 레이아웃이 권장됩니다.
이 방법을 사용하면 sbt 플러그인을 사용하여 로컬 환경에서 템플릿을 테스트하고 GitHub에 push 할 수있게 됩니다.
새 템플릿 프로젝트를 시작하는 가장 쉬운 방법은 제공되는 전용 Giter8 템플릿을 사용하는 것입니다.

    $ g8 foundweekends/giter8.g8

이 명령은 `src/main/g8` 안에 임시 소스가 들어간 sbt프로젝트를 생성합니다.
`default.properties` 라는 Java 프로퍼티 파일이 템플릿 필드 및 기본 값을 정의 합니다. 

### default.properties

 `default.properties` 는 `project/` 디렉토리 아래 또는 템플릿의 루트에 두는것이 가능합니다.
 속성은 간단한 키와 그것을 대채할 값의 쌍으로 이루어져 있습니다.

Giter8 템플릿에 실제 적용하는 것은 [Scalasti][scalasti] 로 테이핑되어진 [StringTemplate][st]에서
소스파일의 템플릿 필드 `$` 로 묶어 씁니다. 구체적인 예로 설명하면 `classname`이라는 필드는 소스내에서

    class $classname$ {

와 같은 형태로 표기됩니다.

[scalasti]: http://bmc.github.com/scalasti/
[st]: https://www.stringtemplate.org/

이 템플릿 필드는 다른 필드의 기본값을 정의하는데에도 사용할 수 있습니다.
예를 들어, 사용자의 GitHub ID 를 사용하여, URL을 만드는것이 가능합니다.

```
name = URL Builder
github_id=githubber
developer_url=https://github.com/$github_id$
project_url=https://github.com/$github_id$/$name;format="norm"$
```

대화 형 모드에서는 다음과 같은 내용이 프롬프트됩니다.

```
name [URL Builder]: my-proj
github_id [githubber]: n8han
project_url [https://github.com/n8han/my-proj]:
developer_url [https://github.com/n8han]:
```

### name 필드
 `name` 필드는 Giter8 특수 취급되는 필드이다. 이것은 프로젝트 이름에 사용되어지고, 
g8 런타임은이 이 `name`을 사용하여 템플릿이 출력되는 디렉토리를 생성한다. (공백과 대문자 대체된다).
`name`필드가 템플릿 내에 지정되지 않으면, g8은 사용자의 현재 디렉토리에 출력합니다. 
두 경우 모두, 템플릿 소스 디렉토리의 출력 디렉토리에 복사된다. 또한 파일 및 디렉토리 이름도 템플릿 확장의 대상이됩니다. 
예를 들면 아래와 같습니다.

    src/main/g8/src/main/scala/$classname$.scala

### package 필드
`package` 필드는 만약 정의되어 있으면, 사용자의 소스 패키지 이름으로 처리되게 됩니다.
`$package$` 와 뒤이어 붙는 이름은 패키지 디렉토리 구조로 전개됩니다.
예를 들어, `net.databinder` 는 `net/databinder` 가 됩니다.

### verbatim 필드
`verbatim` 필드는 만약 정의되어 있으면 띄어쓰기 한칸을 구분자로하는 파일 패턴의 목록 (예 *.html *.js)으로 정해져있습니다. 
`verbatim` 패턴과 일치하는 파일은 String Template 처리에서 제외됩니다.

### Maven 속성
**Maven 속성** 을 사용하여 Central Maven Repository를 쿼리 할 수 있습니다.
특정 버전을 쓰는것 (그리고 배포마다 템플릿을 업데이트) 대신에 라이브러리 이름 만 쓰고 
Giter8가 최신 버전을 Maven Central에서 찾고 설정 하도록 하는 방법이 있습니다.

이 속성의 문법은 `maven(groupId, artifactId)` 와 같다. 
여기서 주의해야 할 점은 Scala 프로젝트는 종종 Scala 버전이 artifact id에 들어가는 형태로 공개되는 것 입니다.
따라서 예를 들어 최신 Unfiltered 라이브러리를 지정하려면 다음과 같이 쓸 필요가 있습니다.

```
name = My Template Project
description = Creates a giter8 project template.
unfiltered_version = maven(ws.unfiltered, unfiltered_2.11)
```

### root 레이아웃

실험적인 레이아웃으로 root 레이아웃이라는 것이 있고, 이것은 GitHub 프로젝트의 루트 디렉토리를 템플릿의 루트로 사용 합니다.
그러나, 이 방법도 `project` 안에 템플릿 필드를 사용할 수 없게되므로 적용 범위는 매우 제한되어 있습니다. 
sbt 빌드는없는 템플릿이나 필드가 없는 템플릿에 도움이 될지도 모릅니다.


### 템플릿 필드의 포맷

Giter8 템플릿 필드를 포맷하는 방법을 몇 가지 준비하고있습니다. 포맷 옵션은 필드를 참조 할 때 지정 가능합니다.
예를 들어, `name` 필드를 Upper Camel Case로 포맷하려면,

    $name;format="Camel"$

포맷 옵션:

    upper    | uppercase       : 전부 대문자
    lower    | lowercase       : 전부 소문자
    cap      | capitalize      : 최초 첫글자를 대문자화
    decap    | decapitalize    : 최초 첫글자를 소문자화
    start    | start-case      : 각 워드의 최초 문자를 대문자화
    word     | word-only       : a-zA-Z0-9_ 만 허용. 나머지는 전부 소거
    space    | word-space      : a-zA-Z0-9_ 가 아닌 문자는 빈문자(white-space)처리
    Camel    | upper-camel     : 대문자를 카멜케이스 (start-case, word-only)
    camel    | lower-camel     : 소문자를 카멜케이스 (start-case, word-only, decapitalize)
    hyphen   | hyphenate       : 빈문자(white-space)를 하이픈(-) 처리
    norm     | normalize       : 전체문자를 소문자하고 빈문자(white-space)를 하이픈(-)처리 (lowercase, hyphenate)
    snake    | snake-case      : 빈문자(white-space)와 마침표를(.)를 언더스코어(_)처리
    package  | package-naming  : 빈문자(white-space)를 마침표(.)로 변경
    packaged | package-dir     : 마침표(.)를 슬래시(/)로 변경 (net.databinder -> net/databinder)
    random   | generate-random : 랜덤 문자열을 추가

`name` 필드에 `My Project` 값이 지정된 경우에 어떻게 포맷되는지 봅시다.:

    $name$ -> "My Project"
    $name;format="camel"$ -> "myProject"
    $name;format="Camel"$ -> "MyProject"
    $name;format="normalize"$ -> "my-project"
    $name;format="lower,hyphen"$ -> "my-project"

쉼표로 구분된 여러 형식을 지정할수 있는것에 주목, 이경우 주어진 순서대로 처리됩니다.
파일이나 디렉토리 이름을 포맷 옵션을 전달하려면 밑줄을 두 개 연결 한 것을 사용합니다.
예를들면,`$organization__packaged$` 라는 이름이 붙은 디렉토리는 기본제공 된 `package`과 마찬가지로
`org.somewhere` 를 `org/somewhere` 로 변환합니다.
`$name__Camel$.scala` 라는 이름이 붙은 파일은、`name`이 `awesome project` 로 설정되어 있으면
`AwesomeProject.scala` 라는 파일을 생성합니다.


### 로컬환경에서 템플릿 테스트

`g8`명령에 `file://`을 사용하여 템플릿을 전달할 수있으므로, 템플릿을 로컬 파일 시스템에 둘 수있습니다.
 기존의 파일이 있어도 덮어 쓰기 `--force`옵션을 함께 사용하여 템플릿을 변경하면서 테스트 할 수있습니다.

예를들면, Unfiltered 템플릿을 로컬에 clone 했다고 할때, 다음과 같은 명령을 실행 할 수 있습니다.

    $ g8 file://unfiltered.g8/ --name=uftest --force

터미널의 다른 창으로 이 템플릿을 테스트 해봅니다.

    $ cd uftest/
    $ sbt
    > ~ compile

템플릿을 변경하려면, `.g8` 디렉토리 아래에 파일을 저장하고, 처음 사용한 터미널에서, 아래 명령을 다시 실행 합니다.

    $ g8 file://unfiltered.g8/ --name=uftest --force

`uftest` 의 sbt 세션은 `~ compile` 명령을 대기하고 있기 때문에, 변경을 감지하여 자동으로 재 컴파일이 일어납니다.

### Giter8Plugin 을 사용

Giter8는, 템플릿의 테스트를 위한 sbt 플러그인을 제공하고, 이것을 사용하면,
GitHub 브랜치에 템플릿을 push 하기전에 테스트를 해볼 수 있습니다.
위의 가이드대로, `foundweekends/giter8.g8` 템플릿을 사용하고 있다면 이미 구성되어 있습니다.

이미 사용하고 있는 템플릿을 현재 플러그인으로 업그레이드 하기위해서는 
`project/giter8.sbt` 에 아래 내용을 추가합니다.

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "0.12.0")
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

GitHub에 템플릿을 push 하면 즉시 실제  런타임을 사용하여 테스트를하는 것이 가능합니다. 
(프로젝트 이름에 `.g8` 확장자를 잊지 않도록) 완성되면 템플릿 프로젝트 [wiki][wiki] 에 추가하여
다른 Giter8 사용자가 발견되도록 하여야 합니다.

  [scripted]: https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
  [wiki]: https://github.com/foundweekends/giter8/wiki/giter8-templates


Scaffolding 플러그인
---------------------

Giter8는 scaffolding 용 sbt 플러그인도 제공하고 있습니다.

### scaffold 플러그인의 사용

아래내용을 `project/scaffold.sbt` 파일에 적용:

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.12.0")
```

이제 sbt 쉘에서 `g8Scaffold` 명령을 사용할 수있게 되었습니다.
또한 탭 완성을 사용하여 사용 가능한 템플릿을 확인 해볼 수있습니다.

```
> g8Scaffold <TAB>
controller   global       model
```

템플릿 마찬가지로 scaffold 처리에 필요한 필드는 순서대로 프롬프트됩니다 :

```
> g8Scaffold controller
className [Application]:
```

### scaffold 를 만드는 방법

g8 런타임은 GitHub 프로젝트가 있을 때, `src/main/scaffolds` 이하의 scaffold 를 찾습니다.
`src/main/scaffolds` 안의 각 디렉토리는 각개의 scaffold 로서, 디렉토리명에 따라, 
sbt 쉘에서 엑세스 가능합니다. 일반적으로 템플릿과 마찬가지로, scaffold 디렉토리에, `default.properties`파일을 두고
필드값을 정의하는것이 가능합니다. 여기에서도 `name` 은 특별한 필드명으로서,
혹시 `name` 필드가 있다면, scaffold 는 `name` 을 기반으로 디렉토리가 생성되고, 그 아래에, scaffold 의 소스 디렉토리를
복제하는 방식으로 하위디렉토리 구조가 형성됩니다.
 
템플릿으로 사용된 scaffold는 `<project_root>/.g8`에 저장됩니다.

```
$ ls sample/.g8
total 0
drwxr-xr-x   5 jtournay  staff   170B Aug  6 03:21 .
drwxr-xr-x  11 jtournay  staff   374B Aug  6 05:29 ..
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 controller
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 global
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 model
```

이를 이용하여 임의의 sbt 프로젝트가있을 때 .g8디렉토리를 만들기위한 자신의 scaffold를 만드는 것도 가능합니다.


기여
-----

### giter8 로컬버젼 설치

로컬에서 giter8 작업을 할 때 pull 요청을 하기 전에 
변경 사항을 시험해보기를 원할 것입니다. 아래 방법을 참고하세요.


Giter8은 배포를 위해 [conscript] 를 사용합니다.  [공식페이지]에서 conscript에 대한 상세 정보를 확인 할 수 있습니다.

#### Fixing `PATH`:

conscript와 함께 giter8을 설치하기 전에 conscript 디렉토리가 기본 설치 경로보다 우선 순위가 높아야 합니다.

설치된 giter8 일반 버젼을 삭제하거나, `PATH`의 `~/.conscript/bin` 경로 앞에 추가하여, 우선순위를 변경 합니다.

#### 로컬버젼 설치:

- `build.sbt` 파일안에, `g8version`를 변경하세요. i.e. by adding `"-SNAPSHOT"`;
- `publishLocal` 실행;
- `build.sbt` 에서 버젼정보 참고해서, 쉘에서 `cs --local foundweekends/giter8/<YOUR_VERSION>` 실행.

#### 갱신:

- `publishLocal` 실행;
- 쉘에서 `cs --clean-boot`. 실행

#### 일반 버젼으로 변경:

쉘에서 `cs foundweekends/giter8`. 실행

[official page]: https://github.com/foundweekends/conscript
[conscript]: https://www.foundweekends.org/conscript/
