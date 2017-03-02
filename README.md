## Classpath Shrinker: a scalac plugin to detect unused classpath entries

Proof of concept plugin to scan the compiler's symbol table after a full clean
build in order to find which JARs are on your classpath but are not referred to.

### Using

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