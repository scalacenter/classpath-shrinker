lazy val root = project.aggregate(plugin, example)

scalaVersion in ThisBuild := "2.12.1"

crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1")

organization in ThisBuild := "ch.epfl.scala"

name := "classpath-shrinker"

lazy val plugin = project.settings (
  name := "classpath-shrinker-plugin",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  publishArtifact in Compile := false
)

lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar = (Keys.`package` in (plugin, Compile)).value
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

// A regular module with the application code.
lazy val example = project.settings(usePluginSettings).settings(
  libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5",
  libraryDependencies += "com.google.guava" % "guava" % "21.0"
)
