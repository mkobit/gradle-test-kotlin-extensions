package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

internal class GradleRunnerBuildExtensionsTest {

  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var mockBuildResult: BuildResult
  private lateinit var mockProjectDirectory: Path

  @BeforeEach
  internal fun setUp() {
    mockGradleRunner = mock()
    mockBuildResult = mock()
    mockProjectDirectory = mock()

    val mockFile: File = mock()
    whenever(mockFile.toPath()).thenReturn(mockProjectDirectory)
    whenever(mockGradleRunner.projectDir).thenReturn(mockFile)
  }

  @Test
  internal fun `when 'build()' is called the project directory is a part of the build result`() {
    whenever(mockGradleRunner.arguments).thenReturn(emptyList())
    whenever(mockGradleRunner.build()).thenReturn(mockBuildResult)

    val result = mockGradleRunner.build("task1", "task2")

    verify(mockGradleRunner).build()
    assertThat(result.projectDir)
        .isEqualTo(mockProjectDirectory)
  }

  @Test
  internal fun `when 'build()' is called with arguments then those arguments are appended to the existing arguments`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.build()).thenReturn(mockBuildResult)

    mockGradleRunner.build("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).withArguments(original + listOf("task1", "task2"))
    inOrder.verify(mockGradleRunner).build()
  }

  @Test
  internal fun `when 'build()' is called then the arguments are reset afterwards`() {
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
  internal fun `when 'build()' throws an exception then the arguments are still restored`() {
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
  internal fun `when 'buildAndFail()' is called with arguments then those arguments are appended to the existing arguments`() {
    val original = listOf("some", "args")
    whenever(mockGradleRunner.arguments).thenReturn(original)
    whenever(mockGradleRunner.buildAndFail()).thenReturn(mockBuildResult)

    mockGradleRunner.buildAndFail("task1", "task2")

    val inOrder = inOrder(mockGradleRunner)
    inOrder.verify(mockGradleRunner).withArguments(original + listOf("task1", "task2"))
    inOrder.verify(mockGradleRunner).buildAndFail()
  }

  @Test
  internal fun `when 'buildAndFail()' is called then the arguments are reset afterwards`() {
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
  internal fun `when 'buildAndFail()' throws an exception then the arguments are still restored`() {
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

  @Test
  internal fun `when 'buildAndFail()' is called the project directory is a part of the build result`() {
    whenever(mockGradleRunner.arguments).thenReturn(emptyList())
    whenever(mockGradleRunner.buildAndFail()).thenReturn(mockBuildResult)

    val result = mockGradleRunner.buildAndFail("task1", "task2")

    verify(mockGradleRunner).buildAndFail()
    assertThat(result.projectDir)
        .isEqualTo(mockProjectDirectory)
  }
}
