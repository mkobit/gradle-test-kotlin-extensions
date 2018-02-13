package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BuildResultExtensionsTest {

  private lateinit var mockBuildResult: BuildResult

  @BeforeEach
  internal fun setUp() {
    mockBuildResult = mock()
  }

  @Test
  internal fun `can get a task by path using indexed access`() {
    val path = ":somepath"
    val buildTask: BuildTask = mock()
    whenever(mockBuildResult.task(path)).thenReturn(buildTask)

    val result = mockBuildResult[path]
    assertThat(result)
        .isSameAs(buildTask)
    verify(mockBuildResult).task(path)
  }

  @Test
  internal fun `can get multiple tasks by path using indexed access`() {
    val firstPath = ":first"
    val secondPath = ":second"
    val thirdPath = ":third"
    val firstBuildTask: BuildTask = mock()
    val secondBuildTask: BuildTask = mock()
    whenever(mockBuildResult.task(firstPath)).thenReturn(firstBuildTask)
    whenever(mockBuildResult.task(secondPath)).thenReturn(secondBuildTask)
    whenever(mockBuildResult.task(thirdPath)).thenReturn(null)

    val result = mockBuildResult[firstPath, secondPath, thirdPath]
    assertThat(result)
        .containsExactly(firstBuildTask, secondBuildTask, null)
  }

  @Test
  internal fun `can get the tasks by outcome using indexed access`() {
    val outcome = TaskOutcome.SUCCESS
    val tasksWithOutcome: List<BuildTask> = emptyList()
    whenever(mockBuildResult.tasks(outcome)).thenReturn(tasksWithOutcome)

    val result = mockBuildResult[outcome]
    assertThat(result)
        .isSameAs(tasksWithOutcome)
    verify(mockBuildResult).tasks(outcome)
  }
}
