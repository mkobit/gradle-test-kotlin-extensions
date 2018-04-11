package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path

/**
 * An extension of a [BuildResult].
 */
interface KBuildResult : BuildResult {
  /**
   * Retrieves the task at the given path.
   * @param taskPath the path of the task, for example `":build"`.
   */
  override fun task(taskPath: String): KBuildTask?

  /**
   * Retrieves the tasks that have the provided [TaskOutcome]
   */
  override fun tasks(outcome: TaskOutcome): List<KBuildTask>

  /**
   * Retrieves all tasks.
   */
  override fun getTasks(): List<KBuildTask>

  /**
   * Retries all task paths with the provided [TaskOutcome].
   * @param outcome the outcome to filter by
   */
  override fun taskPaths(outcome: TaskOutcome): List<String>

  /**
   * The directory the build was executed in.
   * @see [org.gradle.testkit.runner.GradleRunner.getProjectDir]
   */
  val projectDir: Path
}
