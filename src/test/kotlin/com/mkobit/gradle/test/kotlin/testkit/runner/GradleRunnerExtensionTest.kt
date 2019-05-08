package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.startsWith
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
  internal fun `set project dir with a Path`() {
    val path = Paths.get("/tmp")
    every { mockGradleRunner.withProjectDir(any()) } returns mockGradleRunner
    expectThat(mockGradleRunner.withProjectDir(path))
      .isSameInstanceAs(mockGradleRunner)
    verify {
      mockGradleRunner.withProjectDir(path.toFile())
    }
  }

  @Test
  internal fun `resolve path from projectDir when projectDir unset`() {
    every { mockGradleRunner.projectDir }.returns(null)
    expectThrows<IllegalStateException> {
      mockGradleRunner.resolveFromProjectDir(Paths.get("a"))
    }
  }

  @Test
  internal fun `resolve path from projectDir`() {
    val projectDirPath = Paths.get("a")
    every { mockGradleRunner.projectDir }.returns(projectDirPath.toFile())

    val resolvePath = Paths.get("b")
    val actual = mockGradleRunner.resolveFromProjectDir(resolvePath)
    expectThat(actual)
        .startsWith(projectDirPath)
        .endsWith(resolvePath)
  }

  @Test
  internal fun `project directory as path`() {
    val projectDir = Paths.get("a")
    every { mockGradleRunner.projectDir }.returnsMany(null, projectDir.toFile())
    expect {
      that(mockGradleRunner.projectDirPath).isNull()
      that(mockGradleRunner.projectDirPath).isEqualTo(projectDir)
    }
  }

  @Test
  internal fun `cannot resolve path when projectDir not set`() {
    every { mockGradleRunner.projectDir }.returns(null)

    expectThrows<IllegalStateException> { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }
}
