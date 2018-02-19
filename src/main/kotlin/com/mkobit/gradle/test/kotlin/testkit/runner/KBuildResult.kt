package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * An extension of a [BuildResult].
 */
interface KBuildResult : BuildResult {
  override fun task(taskPath: String): KBuildTask?

  override fun tasks(outcome: TaskOutcome): List<KBuildTask>

  override fun getTasks(): List<KBuildTask>

  override fun taskPaths(outcome: TaskOutcome): List<String>
}
