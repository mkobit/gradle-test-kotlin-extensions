package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GradleRunnerBuildExtensionsTest {

  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var mockBuildResult: BuildResult

  @BeforeEach
  internal fun setUp() {
    mockGradleRunner = mock()
    mockBuildResult = mock()
  }

  @Test
  internal fun `when 'build' is called with arguments then those arguments are appended to the existing arguments`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.build()).thenReturn(mockBuildResult)

    mockGradleRunner.build("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).withArguments(original + listOf("task1", "task2"))
    inOrder.verify(mockGradleRunner).build()
  }

  @Test
  internal fun `the arguments are reset to the previous arguments after the 'build' call`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.build()).thenReturn(mockBuildResult)

    mockGradleRunner.build("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).build()
    inOrder.verify(mockGradleRunner).withArguments(original)
    inOrder.verifyNoMoreInteractions()
  }

  @Test
  internal fun `the arguments are reset to the previous arguments if the 'build' call throws an exception`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    val exception = RuntimeException("test exception")
    whenever(mockGradleRunner.build()).thenThrow(exception)

    assertThatCode { mockGradleRunner.build("task1", "task2") }
        .describedAs("Rethrows exception")
        .isEqualTo(exception)

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).build()
    inOrder.verify(mockGradleRunner).withArguments(original)
    inOrder.verifyNoMoreInteractions()
  }

  @Test
  internal fun `when 'buildAndFail' is called with arguments then those arguments are appended to the existing arguments`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.buildAndFail()).thenReturn(mockBuildResult)

    mockGradleRunner.buildAndFail("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).withArguments(original + listOf("task1", "task2"))
    inOrder.verify(mockGradleRunner).buildAndFail()
  }

  @Test
  internal fun `the arguments are reset to the previous arguments after the 'buildAndFail' call`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.buildAndFail()).thenReturn(mockBuildResult)

    mockGradleRunner.buildAndFail("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).buildAndFail()
    inOrder.verify(mockGradleRunner).withArguments(original)
    inOrder.verifyNoMoreInteractions()
  }

  @Test
  internal fun `the arguments are reset to the previous arguments if the 'buildAndFail' call throws an exception`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    val exception = RuntimeException("test exception")
    whenever(mockGradleRunner.buildAndFail()).thenThrow(exception)

    assertThatCode { mockGradleRunner.buildAndFail("task1", "task2") }
        .describedAs("Rethrows exception")
        .isEqualTo(exception)

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).buildAndFail()
    inOrder.verify(mockGradleRunner).withArguments(original)
    inOrder.verifyNoMoreInteractions()
  }
}
