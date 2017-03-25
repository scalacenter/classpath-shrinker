## Classpath Shrinker

The Classpath Shrinker is a scalac plugin to detect unused classpath entries.
It was created by [Jason Zaugg](https://github.com/retronym) as a better alternative to [a commit](https://github.com/jvican/scala/commit/8d22990ce32d9215f7e1fdd839f00f651b283744)
which fulfilled the same functionality but required the instrumentation of symbol
initializers.

This plugin is now maintained by [the Scala Center](https://scala.epfl.ch).

The creation of this plugin was motivated by [SCP-009: Improve direct dependency experience](https://github.com/scalacenter/advisoryboard/blob/master/proposals/009-improve-direct-dependency-experience.md),
and complements the improvements to stub error messages [available in 2.12.2](https://github.com/scala/scala/pull/5724)
and [2.11.9](https://github.com/scala/scala/issues/5804).

### Usage

```
$ sbt package
> package
[info] Compiling 1 Scala source to /Users/jz/code/classpath-shrinker/plugin/target/scala-2.12/classes...
[info] Packaging /Users/jz/code/classpath-shrinker/plugin/target/scala-2.12/classpath-shrinker-plugin_2.12-0.1-SNAPSHOT.jar ...
[info] Done packaging.

$ scalac -Xplugin:/Users/jz/code/classpath-shrinker/plugin/target/scala-2.12/classpath-shrinker-plugin_2.12-0.1-SNAPSHOT.jar \
         -classpath guava.jar \
         <sources that don't directly refer to classes in guava.jar>
warning: classpath-shrinker detected the following unused classpath entries:
/Users/jz/.ivy2/cache/com.google.guava/guava/bundles/guava-21.0.jar

```