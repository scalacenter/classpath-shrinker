package io.github.retronym.classpathshrinker

import scala.tools.nsc.Settings
import scala.tools.nsc.classpath.ClassPathFactory

/**
  * Provides compatibility stubs for 2.11 and 2.12 Scala compilers.
  */
trait Compat {
  def getClassPathFrom(settings: Settings) = new ClassPathFactory(settings)
}
