package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultKBuildTaskTest {

  private lateinit var mockBuildTask: BuildTask
  private lateinit var defaultKBuildTask: DefaultKBuildTask

  @BeforeEach
  internal fun setUp() {
    mockBuildTask = mock()
    defaultKBuildTask = DefaultKBuildTask(mockBuildTask)
  }

  @Test
  internal fun `returns delegate's build outcome`() {
    val expectedOutcome = TaskOutcome.SUCCESS
    whenever(mockBuildTask.outcome).thenReturn(expectedOutcome)
    assertThat(defaultKBuildTask.outcome).isEqualTo(expectedOutcome)

    verify(mockBuildTask).outcome
    verifyNoMoreInteractions(mockBuildTask)
  }

  @Test
  internal fun `returns delegate's task path`() {
    val expectedPath = ":taskPath"
    whenever(mockBuildTask.path).thenReturn(expectedPath)
    assertThat(defaultKBuildTask.path).isEqualTo(expectedPath)
  }

  @Test
  internal fun `user friendly toString() method`() {
    val expectedOutcome = TaskOutcome.SUCCESS
    val expectedPath = ":taskPath"
    whenever(mockBuildTask.outcome).thenReturn(expectedOutcome)
    whenever(mockBuildTask.path).thenReturn(expectedPath)
    assertThat(defaultKBuildTask.toString()).isEqualTo("DefaultKBuildTask(path=$expectedPath, outcome=$expectedOutcome)")
  }
}
