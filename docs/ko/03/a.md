---
out: formatting.html
---

### 템플릿 필드의 포맷

Giter8 템플릿 필드를 포맷하는 방법을 몇 가지 준비하고있습니다. 포맷 옵션은 필드를 참조 할 때 지정 가능합니다.
예를 들어, `name` 필드를 Upper Camel Case로 포맷하려면,

    \$name;format="Camel"\$

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

    \$name\$ -> "My Project"
    \$name;format="camel"\$ -> "myProject"
    \$name;format="Camel"\$ -> "MyProject"
    \$name;format="normalize"\$ -> "my-project"
    \$name;format="lower,hyphen"\$ -> "my-project"

쉼표로 구분된 여러 형식을 지정할수 있는것에 주목, 이경우 주어진 순서대로 처리됩니다.
파일이나 디렉토리 이름을 포맷 옵션을 전달하려면 밑줄을 두 개 연결 한 것을 사용합니다.
예를들면,`\$organization__packaged\$` 라는 이름이 붙은 디렉토리는 기본제공 된 `package`과 마찬가지로
`org.somewhere` 를 `org/somewhere` 로 변환합니다.
`\$name__Camel\$.scala` 라는 이름이 붙은 파일은、`name`이 `awesome project` 로 설정되어 있으면
`AwesomeProject.scala` 라는 파일을 생성합니다.
