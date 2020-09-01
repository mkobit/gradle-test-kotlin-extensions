package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isSameInstanceAs

internal class BuildResultExtensionsTest {

  private lateinit var mockBuildResult: BuildResult

  @BeforeEach
  internal fun setUp() {
    mockBuildResult = mockk()
  }

  @Test
  internal fun `can get a task by path using indexed access`() {
    val path = ":somepath"
    val buildTask: BuildTask = mockk()
    every { mockBuildResult.task(path) }.returns(buildTask)

    val result = mockBuildResult[path]
    assertThat(result)
      .isSameAs(buildTask)
    verify { mockBuildResult.task(path) }
  }

  @Test
  internal fun `can get multiple tasks by path using indexed access`() {
    val firstPath = ":first"
    val secondPath = ":second"
    val thirdPath = ":third"
    val firstBuildTask: BuildTask = mockk()
    val secondBuildTask: BuildTask = mockk()
    every { mockBuildResult.task(firstPath) }.returns(firstBuildTask)
    every { mockBuildResult.task(secondPath) }.returns(secondBuildTask)
    every { mockBuildResult.task(thirdPath) }.returns(null)

    val result = mockBuildResult[firstPath, secondPath, thirdPath]
    expectThat(result)
      .containsExactly(firstBuildTask, secondBuildTask, null)
  }

  @Test
  internal fun `can get the tasks by outcome using indexed access`() {
    val outcome = TaskOutcome.SUCCESS
    val tasksWithOutcome: List<BuildTask> = emptyList()
    every { mockBuildResult.tasks(outcome) }.returns(tasksWithOutcome)

    val result = mockBuildResult[outcome]
    expectThat(result)
      .isSameInstanceAs(tasksWithOutcome)
    verify { mockBuildResult.tasks(outcome) }
  }
}
