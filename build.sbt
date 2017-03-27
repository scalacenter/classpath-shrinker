lazy val commonSettings = Seq(
  scalaVersion in ThisBuild := "2.12.1",
  crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1"),
  organization in ThisBuild := "ch.epfl.scala"
)

lazy val testDependencies = Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  // Depend on coursier to resolve unused classpath entries
  "io.get-coursier" %% "coursier" % "1.0.0-M15" % "test",
  "io.get-coursier" %% "coursier-cache" % "1.0.0-M15" % "test"
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  bintrayOrganization := Some("scalacenter"),
  bintrayRepository := "releases",
  bintrayPackageLabels := Seq("scalac",
                              "plugin",
                              "scp-009",
                              "direct",
                              "dependency"),
  publishTo := (publishTo in bintray).value,
  publishArtifact in Test := false,
  licenses := Seq(
    // Scala Center license... BSD 3-clause
    "BSD" -> url("http://opensource.org/licenses/BSD-3-Clause")
  ),
  homepage := Some(url("https://github.com/scalacenter/classpath-shrinker")),
  autoAPIMappings := true,
  startYear := Some(2017),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalacenter/classpath-shrinker"),
      "scm:git:git@github.com:scalacenter/classpath-shrinker.git"
    )
  ),
  developers := List(
    Developer("retronym",
              "Jason Zaugg",
              "jason.zaugg@lightbend.com",
              url("http://github.com/retronym")),
    Developer("jvican",
              "Jorge Vicente Cantero",
              "jorge.vicentecantero@epfl.ch",
              url("http://github.com/jvican"))
  )
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val root = project.aggregate(plugin, example).settings(commonSettings)

def inCompileAndTest(ss: Setting[_]*): Seq[Setting[_]] =
  Seq(Compile, Test).flatMap(inConfig(_)(ss))

val scalaPartialVersion =
  Def.setting(CrossVersion partialVersion scalaVersion.value)

lazy val plugin = project.settings(
  name := "classpath-shrinker",
  scalaVersion in ThisBuild := "2.12.1",
  crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1"),
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies ++= testDependencies,
  testOptions in Test ++= List(Tests.Argument("-v"), Tests.Argument("-s")),
  publishSettings,
  // Generate toolbox classpath while compiling
  resourceGenerators in Compile += generateToolboxClasspath.taskValue,
  resourceGenerators in Test += Def.task {
    val options = {
      val jar = (Keys.`package` in Compile).value
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
    val stringOptions = options.filterNot(_ == "-Ydebug").mkString(" ")
    val resourceDir = (resourceDirectory in Test).value
    val pluginOptionsFile = resourceDir / "toolbox.plugin"
    IO.write(pluginOptionsFile, stringOptions)
    List(pluginOptionsFile.getAbsoluteFile)
  }.taskValue,
  inCompileAndTest(unmanagedSourceDirectories ++= {
    scalaPartialVersion.value.collect {
      case (2, y) if y == 11 => new File(scalaSource.value.getPath + "-2.11")
      case (2, y) if y >= 12 => new File(scalaSource.value.getPath + "-2.12")
    }.toList
  })
)

/* Write all the compile-time dependencies of the spores macro to a file,
 * in order to read it from the created Toolbox to run the neg tests. */
lazy val generateToolboxClasspath = Def.task {
  val scalaBinVersion = (scalaBinaryVersion in Compile).value
  val targetDir = (target in Compile).value
  val compiledClassesDir = targetDir / s"scala-$scalaBinVersion/classes"
  val testClassesDir = targetDir / s"scala-$scalaBinVersion/test-classes"
  val libraryJar = scalaInstance.value.libraryJar.getAbsolutePath
  val classpath = s"$compiledClassesDir:$testClassesDir:$libraryJar"
  val resourceDir = (resourceDirectory in Compile).value
  resourceDir.mkdir() // In case it doesn't exist
  val toolboxTestClasspath = resourceDir / "toolbox.classpath"
  IO.write(toolboxTestClasspath, classpath)
  List(toolboxTestClasspath.getAbsoluteFile)
}

// A regular module with the application code.
lazy val example = project
  .settings(
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5",
    libraryDependencies += "com.google.guava" % "guava" % "21.0",
    noPublish,
    scalacOptions in Compile ++= {
      val jar = (Keys.`package` in (plugin, Compile)).value
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )
