package io.github.retronym.classpathshrinker

import java.io.File
import java.nio.file.Paths

import coursier.maven.MavenRepository
import coursier.{Cache, Dependency, Fetch, Resolution}

import scala.reflect.internal.util.{BatchSourceFile, NoPosition}
import scala.reflect.io.VirtualDirectory
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc.{CompilerCommand, Global, Settings}
import scalaz.concurrent.Task

object TestUtil {
  import scala.language.postfixOps

  /** Evaluate using global instance instead of toolbox because toolbox seems
    * to fail to typecheck code that comes from external dependencies. */
  def eval(code: String, compileOptions: String = ""): StoreReporter = {
    // TODO: Optimize and cache global.
    val options = CommandLineParser.tokenize(compileOptions)
    val reporter = new StoreReporter()
    val settings = new Settings(println)
    val _ = new CompilerCommand(options, settings)
    settings.outputDirs.setSingleOutput(new VirtualDirectory("(memory)", None))
    val global = new Global(settings, reporter)
    val run = new global.Run
    val toCompile = new BatchSourceFile("<wrapper-init>", code)
    run.compileSources(List(toCompile))
    reporter
  }

  def getResourceContent(resourceName: String): String = {
    val resource = getClass.getClassLoader.getResource(resourceName)
    val file = scala.io.Source.fromFile(resource.toURI)
    file.getLines.mkString
  }

  lazy val toolboxClasspath: String = getResourceContent("toolbox.classpath")
  lazy val toolboxPluginOptions: String = getResourceContent("toolbox.plugin")

  def createBasicCompileOptions(classpath: String, usePluginOptions: String) =
    s"-classpath $classpath $usePluginOptions"

  def existsWarning(expectedWarning: String,
                    reporter: StoreReporter): Boolean = {
    def hasDetectionWarning: Boolean = {
      reporter.infos.exists { info =>
        info.severity.id == reporter.WARNING.id &&
          info.msg.startsWith("Detected the following unused classpath entries")
      }
    }

    reporter.infos.exists { info =>
      info.severity.id == reporter.WARNING.id && info.msg == expectedWarning
    } || (expectedWarning.isEmpty && !hasDetectionWarning)
  }

  def prettyPrintErrors(reporter: StoreReporter): String = {
    reporter.infos
      .map { info =>
        if (info.pos == NoPosition) info.msg
        else s"""[${info.pos.source}]:${info.pos.line}: ${info.msg}"""
      }
      .mkString("\n")
  }

  def expectWarning(expectedWarning: String,
                    compileOptions: String = "",
                    extraClasspath: Seq[String])(code: String): Unit = {
    val fullClasspath: String = {
      val extraClasspathString = extraClasspath.mkString(":")
      if (toolboxClasspath.isEmpty) extraClasspathString
      else s"$toolboxClasspath:$extraClasspathString"
    }
    val basicOptions =
      createBasicCompileOptions(fullClasspath, toolboxPluginOptions)
    val reporter = eval(code, s"$basicOptions $compileOptions")
    assert(
      existsWarning(expectedWarning, reporter), {
        val errors = prettyPrintErrors(reporter)
        s"Expected warning does not exist." +
          s"Found:\n$errors\nExpected:\n$expectedWarning"
      }
    )
  }

  object Coursier {
    private final val repositories = Seq(
      Cache.ivy2Local,
      MavenRepository("https://repo1.maven.org/maven2")
    )

    def getArtifacts(deps: Seq[Dependency]): Seq[String] =
      getArtifacts(deps, toAbsolutePath)

    def getArtifactsRelative(deps: Seq[Dependency]): Seq[String] =
      getArtifacts(deps, toRelativePath)

    private def getArtifacts(deps: Seq[Dependency], fileToString: File => String): Seq[String] = {
      val toResolve = Resolution(deps.toSet)
      val fetch = Fetch.from(repositories, Cache.fetch())
      val resolution = toResolve.process.run(fetch).run
      val resolutionErrors = resolution.errors
      if (resolutionErrors.nonEmpty)
        sys.error(s"Modules could not be resolved:\n$resolutionErrors.")
      val errorsOrJars = Task
        .gatherUnordered(resolution.artifacts.map(Cache.file(_).run))
        .unsafePerformSync
      val onlyErrors = errorsOrJars.filter(_.isLeft)
      if (onlyErrors.nonEmpty)
        sys.error(s"Jars could not be fetched from cache:\n$onlyErrors")
      errorsOrJars.flatMap(_.map(fileToString).toList)
    }

    private def toAbsolutePath(f: File): String =
      f.getAbsolutePath

    private def toRelativePath(f: File): String =
      Paths.get(System.getProperty("user.dir")).relativize(f.toPath).toString
  }
}
