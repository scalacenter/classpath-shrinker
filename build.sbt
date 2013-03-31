lazy val root = project.aggregate(plugin, main)

scalaVersion in ThisBuild := "2.12.1"

organization in ThisBuild := "io.github.retronym"

name := "classpath-shrinker"

// This subproject contains a Scala compiler plugin that checks for
// value class boxing after Erasure.
lazy val plugin = project.settings (
  name := "classpath-shrinker-plugin",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  publishArtifact in Compile := false
)

// Scalac command line options to install our compiler plugin.
lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar = (Keys.`package` in (plugin, Compile)).value
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

// A regular module with the application code.
lazy val main = project.settings(usePluginSettings).settings(
  libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5",
  libraryDependencies += "com.google.guava" % "guava" % "21.0"
)
