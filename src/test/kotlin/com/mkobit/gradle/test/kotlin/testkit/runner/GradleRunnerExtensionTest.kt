package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
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
    mockBuildResult = mockk()
    mockGradleRunner = mockk {
      every { build() }.returns(mockBuildResult)
      every { buildAndFail() }.returns(mockBuildResult)
    }
  }

  @Test
  internal fun `resolve path from projectDir when projectDir unset`() {
    every { mockGradleRunner.projectDir }.returns(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }

  @Test
  internal fun `resolve path from projectDir`() {
    val projectDirPath = Paths.get("a")
    every { mockGradleRunner.projectDir }.returns(projectDirPath.toFile())

    val resolvePath = Paths.get("b")
    val actual = mockGradleRunner.resolveFromProjectDir(resolvePath)
    assertThat(actual)
        .startsWithRaw(projectDirPath)
        .endsWithRaw(resolvePath)
  }

  @Test
  internal fun `project directory as path`() {
    val projectDir = Paths.get("a")
    every { mockGradleRunner.projectDir }.returnsMany(null, projectDir.toFile())
    assertThat(mockGradleRunner.projectDirPath).isNull()
    assertThat(mockGradleRunner.projectDirPath).isEqualTo(projectDir)
  }

  @Test
  internal fun `cannot resolve path when projectDir not set`() {
    every { mockGradleRunner.projectDir }.returns(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }
}
