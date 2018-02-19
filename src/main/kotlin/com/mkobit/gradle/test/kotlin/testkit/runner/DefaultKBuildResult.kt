package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path

/**
 * Default implementation of [KBuildResult] that delegates to the underlying [BuildResult]
 * @property delegate the actual build result
 */
internal class DefaultKBuildResult(
    override val projectDir: Path,
    private val delegate: BuildResult
) : KBuildResult {
  override fun task(taskPath: String): KBuildTask? = delegate.task(taskPath)?.let(::DefaultKBuildTask)

  override fun tasks(outcome: TaskOutcome): List<KBuildTask> = delegate.tasks(outcome).map(::DefaultKBuildTask)

  override fun getTasks(): List<KBuildTask> = delegate.tasks.map(::DefaultKBuildTask)

  override fun taskPaths(outcome: TaskOutcome): List<String> = delegate.taskPaths(outcome)

  override fun getOutput(): String = delegate.output
}
