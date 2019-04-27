package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.nio.file.Path

internal class DefaultKBuildResultTest {

  private lateinit var mockBuildResult: BuildResult
  private lateinit var mockKBuildTask: KBuildTask
  private lateinit var mockProjectDir: Path
  private lateinit var defaultKBuildResult: DefaultKBuildResult

  @BeforeEach
  internal fun setUp() {
    mockBuildResult = mockk()
    mockProjectDir = mockk()
    mockKBuildTask = mockk()
    defaultKBuildResult = DefaultKBuildResult(mockProjectDir, mockBuildResult)
  }

  @Test
  internal fun `returns delegate's task at task path`() {
    val taskPath = ":myPath"
    every { defaultKBuildResult.task(taskPath) }.returns(null)

    expectThat(defaultKBuildResult.task(taskPath))
      .isNull()
    verifyAll { mockBuildResult.task(taskPath) }
  }

  @Test
  internal fun `returns delegate's tasks for outcome`() {
    val outcome = TaskOutcome.SUCCESS
    every { mockBuildResult.tasks(outcome) }.returns(emptyList())

    expectThat(defaultKBuildResult.tasks(outcome)).isEmpty()
    verifyAll { mockBuildResult.tasks(outcome) }
  }

  @Test
  internal fun `returns delegate's tasks`() {
    every { mockBuildResult.tasks }.returns(emptyList())

    expectThat(defaultKBuildResult.tasks).isEmpty()

    verify { mockBuildResult.tasks }
  }

  @Test
  internal fun `returns delegate's task paths for outcome`() {
    val taskPaths = listOf(":hello")
    val outcome = TaskOutcome.SUCCESS
    every { mockBuildResult.taskPaths(outcome) }.returns(taskPaths)

    expectThat(defaultKBuildResult.taskPaths(outcome)).isSameInstanceAs(taskPaths)
    verify { mockBuildResult.taskPaths(outcome) }
  }

  @Test
  internal fun `returns delegate's output`() {
    val output = "build output"
    every { mockBuildResult.output }.returns(output)

    expectThat(defaultKBuildResult.output).isSameInstanceAs(output)
    verify { mockBuildResult.output }
  }

  @Test
  internal fun `user friendly toString() method`() {
    val pathToStringPlaceholder = "PLACEHOLDER"
    every { mockProjectDir.toString() }.returns(pathToStringPlaceholder)
    every { mockBuildResult.tasks }.returns(emptyList())

    expectThat(defaultKBuildResult.toString())
      .isEqualTo("DefaultKBuildResult(projectDir=$pathToStringPlaceholder, tasks=[])")
  }
}
