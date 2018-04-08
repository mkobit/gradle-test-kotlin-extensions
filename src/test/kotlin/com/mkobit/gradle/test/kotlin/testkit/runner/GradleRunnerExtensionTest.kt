package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class GradleRunnerExtensionTest {

  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var mockBuildResult: BuildResult

  @BeforeEach
  internal fun setUp() {
    mockBuildResult = mock()
    mockGradleRunner = mock {
      on { build() } doReturn mockBuildResult
      on { buildAndFail() } doReturn mockBuildResult
    }
  }

  @Test
  internal fun `resolve path from projectDir when projectDir unset`() {
    whenever(mockGradleRunner.projectDir).thenReturn(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }

  @Test
  internal fun `resolve path from projectDir`() {
    val projectDirPath = Paths.get("a")
    whenever(mockGradleRunner.projectDir).thenReturn(projectDirPath.toFile())

    val resolvePath = Paths.get("b")
    val actual = mockGradleRunner.resolveFromProjectDir(resolvePath)
    assertThat(actual)
        .startsWithRaw(projectDirPath)
        .endsWithRaw(resolvePath)
  }

  @Test
  internal fun `project directory as path`() {
    val projectDir = Paths.get("a")
    whenever(mockGradleRunner.projectDir).thenReturn(null, projectDir.toFile())
    assertThat(mockGradleRunner.projectDirPath).isNull()
    assertThat(mockGradleRunner.projectDirPath).isEqualTo(projectDir)
  }

  @Test
  internal fun `cannot resolve path when projectDir not set`() {
    whenever(mockGradleRunner.projectDir).thenReturn(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }
}
