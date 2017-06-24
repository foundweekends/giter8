---
out: testing.html
---

### ローカル環境でのテンプレートのテスト

`g8` コマンドに対して `file://` URL を使ってテンプレートを渡すことが可能なので、
テンプレートをローカルのファイルシステム上に置くことができる。
これと、既存のファイルがあってもプロンプトせずに上書きする `--force` オプションを併用することでテンプレートを変更しながらテストすることができる。

例えば、Unfiltered テンプレートをローカルに clone してあるとして、以下のようなコマンドを実行できる:

    \$ g8 file://unfiltered.g8/ --name=uftest --force

ターミナルの別窓で、このようにテンプレートをテストする。

    \$ cd uftest/
    \$ sbt
    > ~ compile

テンプレートを変更するには `.g8` ディレクトリ以下にファイルを保存して、最初のターミナルで先程のコマンドを再実行すればいい:

    \$ g8 file://unfiltered.g8/ --name=uftest --force

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
