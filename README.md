# lilib-extension
extension libraries of LIlib

## About

[LegalInformationAnalysis](https://github.com/nil2013/LegalInformationAnalysis) から一部のプログラムを抽象化してまとめたライブラリ。

### lilib-analyzer

判例データに対して一定の分析を行うためのクラス群。

### lilib-supremecourtdb

最高裁判所判例データベースに含まれる判例データを処理するためのクラス群。
主として、最高裁判例データベースへの接続と判例のダウンロード、およびローカルに保存されたそれらの判例の処理と、それらの処理の抽象化を行う。

### lilib-search

最高裁判例データの簡易検索エンジン
システム内で分析を行うに際してデータベースが必要となった場合に利用する。

## Required

[LIlib](https://github.com/nil2013/LIlib): Publishしていないので、PublishLocalする必要がある。

## Usage

lilib-analyzer
```scala
libraryDependencies += "me.nsmr" %% "lilib-analyzer" % "0.1.0-SNAPSHOT"
```

lilib-supremecourtdb
```scala
libraryDependencies += "me.nsmr" %% "lilib-supremecourtdb" % "0.1.0-SNAPSHOT"
```
