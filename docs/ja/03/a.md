---
out: formatting.html
---

### テンプレートフィールドのフォーマット

Giter8 は、テンプレートフィールドをフォーマットする方法をいくつか用意してある。
フォーマットのオプションはフィールドを参照するときに指定可能だ。例えば、`name`
フィールドを upper camel case でフォーマットするには:

    \$name;format="Camel"\$

フォーマットは以下のオプションがある:

    upper    | uppercase       : 全部大文字
    lower    | lowercase       : 全部小文字
    cap      | capitalize      : 最初の文字を大文字化
    decap    | decapitalize    : 最初の文字を小文字化
    start    | start-case      : 各ワードの最初の文字を大文字化
    word     | word-only       : 非ワード文字の除去 (a-zA-Z0-9_ のみ)
    Camel    | upper-camel     : 大文字キャメルケース (start-case, word-only)
    camel    | lower-camel     : 小文字キャメルケース (start-case, word-only, decapitalize)
    hyphen   | hyphenate       : 空文字のハイフン化
    norm     | normalize       : 全てを小文字化、空文字はハイフン化 (lowercase, hyphenate)
    snake    | snake-case      : 空文字とドットのアンダースコア化
    packaged | package-dir     : ドットのスラッシュ化 (net.databinder -> net/databinder)
    random   | generate-random : ランダム文字列の追加

`name` フィールドに `My Project` という値が指定されたときにどうフォーマットされるのかをみてみよう:

    \$name\$ -> "My Project"
    \$name;format="camel"\$ -> "myProject"
    \$name;format="Camel"\$ -> "MyProject"
    \$name;format="normalize"\$ -> "my-project"
    \$name;format="lower,hyphen"\$ -> "my-project"

コンマ区切り複数のフォーマットを指定可能なことにも注目してほしい。その場合は、与えられた順序で評価される。

ファイルやディレクトリ名にフォーマットオプションを渡すにはアンダースコアを 2つつなげたものを使う。
例えば、`\$organization__packaged\$` という名前のついたディレクトリは、組み込みの `package` フィールド同様に
`org.somewhere` を `org/somewher` に変換する。
`\$name__Camel\$.scala` という名前のついたファイルは、`name` が `awesome project` であるとき、
`AwesomeProject.scala` というファイルを生成する。
