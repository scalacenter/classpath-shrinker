package io.github.retronym.classpathshrinker

import java.io.File
import java.net.URI

import scala.tools.nsc.classpath.ClassPathFactory
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}
import scala.tools.util.PathResolver

class ClassPathShrinker(val global: Global) extends Plugin {

  val name = "classpath-shrinker"
  val description = "Warns about classpath entries that are not directly needed."
  val components = List[PluginComponent](Component)

  private object Component extends PluginComponent {
    val global = ClassPathShrinker.this.global
    import global._

    override val runsAfter = List("jvm")

    val phaseName = ClassPathShrinker.this.name

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {

        val distinctJars = completedTopLevels.flatMap(x => x.associatedFile.underlyingSource).toSet
        val usedClasspathStrings = distinctJars.toList.map(_.toString).sorted
        val userClasspathURLs = new ClassPathFactory(settings).classesInExpandedPath(settings.classpath.value).flatMap(_.asURLs)
        //        println("\nClasspath:\n" + global.classPath.asClassPathStrings.sorted.mkString("\n"))
        def toJar(u: URI): Option[File] = util.Try { new File(u) }.toOption.filter(_.getName.endsWith(".jar"))
        val userClasspathStrings = userClasspathURLs.flatMap(x => toJar(x.toURI)).map(_.getPath).toList
        val unneededClasspath = userClasspathStrings.filterNot(s => usedClasspathStrings.contains(s))
        if (unneededClasspath.nonEmpty) {
          warning("classpath-shrinker detected the following unused classpath entries: \n" + unneededClasspath.mkString("\n"))
        }
      }
    }
  }

  import global._

  private def walkTopLevels(root: Symbol): Iterator[Symbol] = {
    def safeInfo(sym: Symbol): Type = if (sym.hasRawInfo && sym.rawInfo.isComplete) sym.info else NoType
    def packageClassOrSelf(sym: Symbol): Symbol = if (sym.hasPackageFlag && !sym.isModuleClass) sym.moduleClass else sym

    if (root.hasPackageFlag)
      safeInfo(packageClassOrSelf(root)).decls.iterator.flatMap(x =>
        if (x == root) Nil else walkTopLevels(x)
      ) else Iterator(root)
  }

  private def completedTopLevels: Iterator[Symbol] = {
    def isFromClasspath(x: Symbol) = x.associatedFile != reflect.io.NoAbstractFile && x.sourceFile == null
    walkTopLevels(RootClass).filterNot(_.hasPackageFlag).filter(isFromClasspath).filter(x => x.hasRawInfo && x.rawInfo.isComplete)
  }
}
