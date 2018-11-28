package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class DefaultKBuildResultTest {

  private lateinit var mockBuildResult: BuildResult
  private lateinit var mockKBuildTask: KBuildTask
  private lateinit var mockProjectDir: Path
  private lateinit var defaultKBuildResult: DefaultKBuildResult

  @BeforeEach
  internal fun setUp() {
    mockBuildResult = mock()
    mockProjectDir = mock()
    mockKBuildTask = mock()
    defaultKBuildResult = DefaultKBuildResult(mockProjectDir, mockBuildResult)
  }

  @Test
  internal fun `returns delegate's task at task path`() {
    val taskPath = ":myPath"
    assertThat(defaultKBuildResult.task(taskPath))
      .isNull()

    verify(mockBuildResult).task(taskPath)
    verifyNoMoreInteractions(mockBuildResult, mockProjectDir)
  }

  @Test
  internal fun `returns delegate's tasks for outcome`() {
    val outcome = TaskOutcome.SUCCESS

    assertThat(defaultKBuildResult.tasks(outcome)).isEmpty()
    verify(mockBuildResult).tasks(outcome)
    verifyNoMoreInteractions(mockBuildResult, mockProjectDir)
  }

  @Test
  internal fun `returns delegate's tasks`() {
    assertThat(defaultKBuildResult.tasks).isEmpty()

    verify(mockBuildResult).tasks
    verifyNoMoreInteractions(mockBuildResult, mockProjectDir)
  }

  @Test
  internal fun `returns delegate's task paths for outcome`() {
    val taskPaths = listOf(":hello")
    val outcome = TaskOutcome.SUCCESS
    whenever(mockBuildResult.taskPaths(outcome)).thenReturn(taskPaths)

    assertThat(defaultKBuildResult.taskPaths(outcome)).isSameAs(taskPaths)
    verify(mockBuildResult).taskPaths(outcome)
    verifyNoMoreInteractions(mockBuildResult, mockProjectDir)
  }

  @Test
  internal fun `returns delegate's output`() {
    val output = "build output"
    whenever(mockBuildResult.output).thenReturn(output)

    assertThat(defaultKBuildResult.output).isSameAs(output)
    verify(mockBuildResult).output
    verifyNoMoreInteractions(mockBuildResult, mockProjectDir)
  }

  @Test
  internal fun `user friendly toString() method`() {
    val pathToStringPlaceholder = "PLACEHOLDER"
    whenever(mockProjectDir.toString()).thenReturn(pathToStringPlaceholder)

    assertThat(defaultKBuildResult.toString())
      .isEqualTo("DefaultKBuildResult(projectDir=$pathToStringPlaceholder, tasks=[])")
  }
}
