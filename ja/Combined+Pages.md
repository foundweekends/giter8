
Giter8
======

Giter8 (読み「ギタレート」「ぎられぃ」) は、Github をはじめとする git レポジトリ上に公開されたテンプレート使ってファイルやディレクトリを生成するためのコマンドラインツールだ。
Scala を使って実装されていて [sbt launcher][launcher] を使って実行されるが、そのアウトプットとしてはどの用途にも使うことができる。

### sbt new との統合

sbt 0.13.13 より、Giter8 は sbt の ["new" コマンド][new]から呼び出すことができる:

```
$ sbt new eed3si9n/hello.g8
```

### クレジット

- 元の実装は (C) 2010-2015 Nathan Hamblen さんおよびコントリビューターの皆さん。
- 2016年以降は foundweekends プロジェクトが拡張を加えている。

Giter8 は Apache 2.0 license によって示される条件下において公開される。

[launcher]: http://www.scala-sbt.org/0.13/docs/Setup.html
[new]: http://www.scala-sbt.org/0.13/docs/sbt-new-and-Templates.html


セットアップ
----------

Giter8 や他の Scala コマンドラインツールは [Conscript][cs]
を使ってインストールすることができる。以下の方法で Conscript を `~/.conscript/bin/cs` としてインストールする:

    curl https://raw.githubusercontent.com/foundweekends/conscript/master/setup.sh | sh

(他のインストール方法もあるので [Conscript のインストール方法][cs]参照。)
`cs` にパスが通った所で、以下の方法で Giter8 をインストール (もしくはアップグレード) できる:

    cs foundweekends/giter8

[cs]: http://www.foundweekends.org/conscript/ja/setup.html

動作を確認するには `g8` をパラメータなしで実行する。
Giter8 とその依存ライブラリがダウンロードされて使用方法が表示されるはずだ。

アップグレードするときも同じ `cs` コマンドを実行すればいい。

Giter8 は OS X のパッケージマネジャーである [Homebrew][] からもインストール可能だ:

    $ brew update && brew install giter8

[Homebrew]: http://brew.sh


使用方法
-------

テンプレートのレポジトリは Github 上では `.g8` で終わる名前を使う規約を勝手に作った。
[Wiki にテンプレートのリスト][wiki]があるので見てほしい。

例えば、[unfiltered/unfiltered.g8][uft] というテンプレートを適用するには以下を実行する:

[uft]: http://github.com/unfiltered/unfiltered.g8
[wiki]: http://github.com/foundweekends/giter8/wiki/giter8-templates

    $ g8 unfiltered/unfiltered.g8

Giter8 はこれを Github 上の `unfiltered/unfiltered.g8`
というレポジトリだと解決してプロジェクトのテンプレートやパラメータを問い合わせる。
他に、git レポジトリをフルネームで指定することも可能だ。

    $ g8 https://github.com/unfiltered/unfiltered.g8.git

テンプレートを実行すると、各パラメータへのプロンプトが表示される。
このときデフォルト値は角括弧で表示される:

    name [My Web Project]: 

何らかの値を書き込むか、エンターキーを押してデフォルト値をそのまま使う。
全ての値が渡されると Giter8 はテンプレートを読み込んで、パラメータを適用して、
ファイルシステムに書き込みを行う。

テンプレートに `name` というパラメータがあると、それはカレントディレクトリ以下のベースディレクトリ名として使われる
(新プロジェクトを生成するときには通常この方法を使う)。
`name` が無ければ、Giter8 はカレントディレクトリにファイルやディレクトリの書き込みを行うが、既存のファイルがある場合はスキップされる。

テンプレートのパラメータに慣れてくるとコマンドライン上から直接指定してプロンプトを回避することも可能だ:

    $ g8 unfiltered/unfiltered.g8 --name=my-new-website

このとき未指定のパラメータはデフォルト値が代入されるので注意。

### プライベートリポジトリ

Giter8 は、git 同様に ssh キーを使ってプライベートリポジトリにアクセスすることができる。


  [CC0]: https://creativecommons.org/publicdomain/zero/1.0/

テンプレートの作り方
-----------------

### テンプレートのライセンスは CC0 1.0 を使う

ソフトウェアのテンプレートのライセンスには、いわゆる「パブリックドメイン」同様に全ての著作権および隣接する権利を放棄する
[CC0 1.0][CC0] を使用することを推奨する。

日本のようなベルヌ条約締結国に在住する場合、著作権は登録無しでも自動的に発生する。
そのため、テンプレートを公開しても使用ライセンスを明示しない限り他人は使用する法的権利が無いことになる。
ややこしいのは寛容 (permissive) と言われている MIT License や Apache License
でも、テンプレートの使用者がテンプレート作者への帰属 (attribution, クレジットに名前を書くこと) を行うことを要請する。
テンプレートに含まれるコードスニペットへの権利を一切放棄するには、国際的なパブリックドメイン相当の CC0 のもとで配布する一択となる。

```
Template license
----------------
Written in <YEAR> by <AUTHOR NAME> <AUTHOR E-MAIL ADDRESS>
[other author/contributor lines as appropriate]

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <http://creativecommons.org/publicdomain/zero/1.0/>.
```

### テンプレートのレイアウト

Github プロジェクトがあるとき、Giter8 ランタイムは以下の 2つのパスにテンプレートを探しに行く:

- もし `src/main/g8` ディレクトリがあれば、`src/main/g8` を使う (`src` レイアウト)
- もし無ければ、トップディレクトリがそのまま使われる (root レイアウト)

### src レイアウト

テンプレートそのものが sbt プロジェクトであることが簡単なので、基本的には src レイアウトが推奨される。
この方法を使うと sbt プラグインを使ってローカル環境でテンプレートをテストしてから Github に push
することが可能になる。

新しいテンプレートプロジェクトを始める簡単な方法はそれ専用の Giter8 テンプレートを使うことだ:

    $ g8 foundweekends/giter8.g8

これは `src/main/g8` 内に仮のソースが入った sbt プロジェクトが作られる。
`default.properties` という Java プロパティファイルがテンプレートのフィールドとそのデフォルト値を定義する。

### default.properties

この `default.properties` は `project/` ディレクトリ下、もしくはテンプレートのルートにに置くことが可能だ。
プロパティは簡単なキーとそれを置換する値のペアから成り立っている。

Giter8 テンプレートに実際の適用を行うのは [Scalasti][scalasti] でラッピングされた [StringTemplate][st] で、
ソースファイル内のテンプレートフィールドは `$` で囲んで書かれる。具体例で説明すると、`classname` というフィールドはソース内では

    class $classname$ {

といったふうに表記される。

[scalasti]: http://bmc.github.com/scalasti/
[st]: http://www.stringtemplate.org/

このテンプレートフィールドは他のフィールドのデフォルト値を定義するのにも使うことができる。
例えば、ユーザの Github id を使って URL を作ることが可能だ:

```
name = URL Builder
github_id=githubber
developer_url=https://github.com/$github_id$
project_url=https://github.com/$github_id$/$name;format="norm"$
```

インタラクティブモードでは以下のようなプロンプトになる:

```
name [URL Builder]: my-proj
github_id [githubber]: n8han
project_url [https://github.com/n8han/my-proj]:
developer_url [https://github.com/n8han]:
```

### name フィールド

もし定義されていれば、`name` フィールドは Giter8 に特殊扱いされるフィールドだ。
これはプロジェクト名に使われることが決められていて、g8
ランタイムはこの名前を用いてテンプレートがアウトプットされるディレクトリを作成する (ただし、空白文字と大文字は置換される)。
`name` フィールドがテンプレートに指定されなければ、g8 はユーザのカレントディレクトリにアウトプットする。
いずれの場合もテンプレートのソースディレクトリ内に入れ子のディレクトリはアウトプットディレクトリ内にも複製される。
また、ファイル名やディレクトリ名もテンプレート展開の対象となる。例えば:

    src/main/g8/src/main/scala/$classname$.scala

### package フィールド

`package` フィールドは、もし定義されていれば、ユーザのソースのパッケージ名となることが決められている。
`$package$` と名前のついたディレクトリはパッケージディレクトリ構造に展開される。
例えば、`net.databinder` は `net/databinder` となる。

### verbatim フィールド

`verbatim` フィールドは、もし定義されていれば、空文字で区切られたファイルパターンのリスト (例えば `*.html *.js`) だと決定されている。
`verbatim` パターンにマッチするファイルは String Template 処理から除外される。

### Maven プロパティ

**Maven プロパティ** を使って Central Maven Repository をクエリすることができる。
特定のバージョンを書く (そしてリリース毎にテンプレートを更新する) 代わりにライブラリ名だけを書いて Giter8
が最新のバージョンを Maven Central から探して設定するという方法がある。

このプロパティの記法は `maven(groupId, artifactId)` だ。
ここで注意するべきなのは Scala プロジェクトは多くの場合 Scala バージョンが artifact id に入る形で公開されていることだ。
そのため、例えば最新の Unfiltered ライブラリを指定するには以下のように書く必要がある:

```
name = My Template Project
description = Creates a giter8 project template.
unfiltered_version = maven(ws.unfiltered, unfiltered_2.11)
```

### root レイアウト

実験的レイアウトとして root レイアウトというのがあって、これは Github プロジェクトのルートディレクトリをテンプレートのルートとして用いる。

ただし、この方法だと `project` 内にテンプレートフィールドを使うことができなくなるので適用範囲は非常に限られている。
sbt ビルドでは無いテンプレートやフィールドを一切持たないテンプレートには有用かもしれない。


### テンプレートフィールドのフォーマット

Giter8 は、テンプレートフィールドをフォーマットする方法をいくつか用意してある。
フォーマットのオプションはフィールドを参照するときに指定可能だ。例えば、`name`
フィールドを upper camel case でフォーマットするには:

    $name;format="Camel"$

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

    $name$ -> "My Project"
    $name;format="camel"$ -> "myProject"
    $name;format="Camel"$ -> "MyProject"
    $name;format="normalize"$ -> "my-project"
    $name;format="lower,hyphen"$ -> "my-project"

コンマ区切り複数のフォーマットを指定可能なことにも注目してほしい。その場合は、与えられた順序で評価される。

ファイルやディレクトリ名にフォーマットオプションを渡すにはアンダースコアを 2つつなげたものを使う。
例えば、`$organization__packaged$` という名前のついたディレクトリは、組み込みの `package` フィールド同様に
`org.somewhere` を `org/somewher` に変換する。
`$name__Camel$.scala` という名前のついたファイルは、`name` が `awesome project` であるとき、
`AwesomeProject.scala` というファイルを生成する。


### ローカル環境でのテンプレートのテスト

`g8` コマンドに対して `file://` URL を使ってテンプレートを渡すことが可能なので、
テンプレートをローカルのファイルシステム上に置くことができる。
これと、既存のファイルがあってもプロンプトせずに上書きする `--force` オプションを併用することでテンプレートを変更しながらテストすることができる。

例えば、Unfiltered テンプレートをローカルに clone してあるとして、以下のようなコマンドを実行できる:

    $ g8 file://unfiltered.g8/ --name=uftest --force

ターミナルの別窓で、このようにテンプレートをテストする。

    $ cd uftest/
    $ sbt
    > ~ compile

テンプレートを変更するには `.g8` ディレクトリ以下にファイルを保存して、最初のターミナルで先程のコマンドを再実行すればいい:

    $ g8 file://unfiltered.g8/ --name=uftest --force

`uftest` にある sbt セッションは `~ compile` コマンドで待機しているので、変更を検知して自動的に再コンパイルされる。

### Giter8Plugin を使う

Giter8 は、テンプレートのテストをするための sbt プラグインを用意していて、これを使えば
Github ブランチにテンプレートをプッシュする前にちゃんとテストをすることが可能となる。
上で推奨される手順にしたがって `foundweekends/giter8.g8` テンプレートを使っていれば既に設定済みのはずだ。

既にあるテンプレートを現行プラグインにアップグレードさせるためには以下を
`project/giter8.sbt` に書き込む:

```scala
// should not use 0.8.0 https://github.com/foundweekends/giter8/issues/292
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % "0.7.2")
```

このプラグインが設定されたテンプレートのベースディレクトリから sbt のシェルに入ると、
`g8Test` というアクションでデフォルトのアウトプットディレクトリ (`target/sbt-test`)
内にテンプレートが適用されて、そのプロジェクトに対して[scriptted test][scripted] がフォークプロセスで実行される。
テストスクリプトは `project/giter8.test` もしくは `src/test/g8/test` として提供できる。
もし無ければ `>test` が使用される。
特に sbt プロジェクトを生成するテンプレートはこの方法でテストできるはずだ。

sbt プロジェクト以外のテンプレートはどう対応するべきだろうか?

    project/default.properties
    TodaysMenu.html

それでも sbt のシェルを使ってテンプレートをテストすることは可能だ。
`g8` というアクションを使えばデフォルトのフィールド値をテンプレートに適用して
`target/g8` ディレクトリ内にファイルを生成する。

Github にテンプレートをプッシュすれば即座に実際の g8 ランタイムを使ってテストをすることが可能だ。
(プロジェクト名に `.g8` 拡張子を付けるのを忘れないように)
完成したらテンプレートプロジェクトを [wiki][wiki] に追加して他の Giter8 ユーザが見つけれるようにしよう。

  [scripted]: http://www.scala-sbt.org/0.13/docs/Testing-sbt-plugins.html
  [wiki]: http://github.com/foundweekends/giter8/wiki/giter8-templates


Scaffolding プラグイン
---------------------

Giter8 は scaffolding 用の sbt プラグインも提供している。

### scaffold プラグインの使用

以下を `project/scaffold.sbt` に書く:

```scala
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.8.0")
```

これで sbt シェルから `g8Scaffold` コマンドを使えるようになる。
タブ補完を使って使用可能なテンプレートを発見できる。

```
> g8Scaffold <TAB>
controller   global       model
```

テンプレート同様に scaffold 処理に必要なフィールドは逐次プロンプトされる:

```
> g8Scaffold controller
className [Application]:
```

### scaffold の作り方

g8 ランタイムは、Github プロジェクトがあるとき、`src/main/scaffolds` 以下に scaffold を探しに行く。
`src/main/scaffolds`内の各ディレクトリは別々の scaffold で、そのディレクトリ名に応じて
sbt シェルからアクセスできる。通常のテンプレート同様に、scaffold のディレクトリ内には `default.properties`
を置いてフィールド値を定義することができる。ここでも `name` は特殊なフィールド名で、
もし `name` フィールドがあれば scaffold は `name` に基いたディレクトリ内に生成され、
その下に scaffold のソースディレクトリを複製する形でサブディレクトリ構造が形成される。

テンプレートとして使用された scaffold は `<project_root>/.g8` に保存される。

```
$ ls sample/.g8
total 0
drwxr-xr-x   5 jtournay  staff   170B Aug  6 03:21 .
drwxr-xr-x  11 jtournay  staff   374B Aug  6 05:29 ..
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 controller
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 global
drwxr-xr-x   4 jtournay  staff   136B Aug  6 03:21 model
```

これを利用して、任意の sbt プロジェクトがあるとき `.g8` ディレクトリを作ることで独自の scaffold を作ることも可能だ。
