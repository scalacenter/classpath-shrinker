package io.github.retronym.classpathshrinker

object ClassPathFeedback {
  def createWarningMsg(unusedEntries: Seq[String]): String = {
    if (unusedEntries.isEmpty) ""
    else {
      val entries = unusedEntries.mkString("\n")
      s"Detected the following unused classpath entries: \n$entries"
    }
  }
}
