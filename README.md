## Classpath Shrinker
[![Build Status](https://platform-ci.scala-lang.org/api/badges/scalacenter/classpath-shrinker/status.svg)](https://platform-ci.scala-lang.org/scalacenter/classpath-shrinker)
[![Maven Central](https://img.shields.io/maven-central/v/ch.epfl.scala/classpath-shrinker_2.12.svg)][search.maven]

The Classpath Shrinker is a scalac plugin to detect unused classpath entries.
It was originally created by [Jason Zaugg](https://github.com/retronym) as a better alternative to [a commit](https://github.com/jvican/scala/commit/8d22990ce32d9215f7e1fdd839f00f651b283744)
which fulfilled the same functionality but required the instrumentation of symbol
initializers.

This plugin is now maintained by [the Scala Center](https://scala.epfl.ch).

The creation of this plugin was motivated by [SCP-009: Improve direct dependency experience](https://github.com/scalacenter/advisoryboard/blob/master/proposals/009-improve-direct-dependency-experience.md),
and complements the improvements to stub error messages [available in 2.12.2](https://github.com/scala/scala/pull/5724)
and [2.11.9](https://github.com/scala/scala/issues/5804).

If you use Pants or Bazel, you may find this compiler plugin useful.

### Add to your project

```scala
resolvers += Resolver.bintrayRepo("scalacenter", "releases")
addCompilerPlugin("ch.epfl.scala" %% "classpath-shrinker" % "0.1.0")
```

Once it's added, it will report if there are unused classpath entries automatically.

Output looks like:

```
[info] Compiling 1 Scala source to /drone/src/github.com/scalacenter/classpath-shrinker/example/target/scala-2.12/classes...
[warn] Detected the following unused classpath entries: 
[warn] /.coursier-cache/https/repo1.maven.org/maven2/com/google/guava/guava/21.0/guava-21.0.jar
[warn] one warning found
```

[search.maven]: http://search.maven.org/#search|ga|1|ch.epfl.scala.classpath-shrinker
