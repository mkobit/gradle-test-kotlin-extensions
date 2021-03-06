package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class DefaultKBuildTaskTest {

  private lateinit var mockBuildTask: BuildTask
  private lateinit var defaultKBuildTask: DefaultKBuildTask

  @BeforeEach
  internal fun setUp() {
    mockBuildTask = mockk()
    defaultKBuildTask = DefaultKBuildTask(mockBuildTask)
  }

  @Test
  internal fun `returns delegate's build outcome`() {
    val expectedOutcome = TaskOutcome.SUCCESS
    every { mockBuildTask.outcome }.returns(expectedOutcome)
    expectThat(defaultKBuildTask.outcome).isEqualTo(expectedOutcome)

    verifyAll { mockBuildTask.outcome }
  }

  @Test
  internal fun `returns delegate's task path`() {
    val expectedPath = ":taskPath"
    every { mockBuildTask.path }.returns(expectedPath)
    expectThat(defaultKBuildTask.path).isEqualTo(expectedPath)
  }

  @Test
  internal fun `user friendly toString() method`() {
    val expectedOutcome = TaskOutcome.SUCCESS
    val expectedPath = ":taskPath"
    every { mockBuildTask.outcome }.returns(expectedOutcome)
    every { mockBuildTask.path }.returns(expectedPath)
    expectThat(defaultKBuildTask.toString())
      .isEqualTo("DefaultKBuildTask(path=$expectedPath, outcome=$expectedOutcome)")
  }
}
