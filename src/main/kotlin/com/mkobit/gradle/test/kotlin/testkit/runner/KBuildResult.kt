package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path

/**
 * An extension of a [BuildResult].
 */
interface KBuildResult : BuildResult {
  override fun task(taskPath: String): KBuildTask?

  override fun tasks(outcome: TaskOutcome): List<KBuildTask>

  override fun getTasks(): List<KBuildTask>

  override fun taskPaths(outcome: TaskOutcome): List<String>

  /**
   * The directory the build was executed in.
   * @see [org.gradle.testkit.runner.GradleRunner.getProjectDir]
   */
  val projectDir: Path
}
