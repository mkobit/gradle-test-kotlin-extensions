package com.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isSuccess
import java.io.File
import java.nio.file.Path
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

  @Nested
  internal inner class BuildExtensionsTest {

    private lateinit var mockGradleRunner: GradleRunner
    private lateinit var mockBuildResult: BuildResult
    private lateinit var mockProjectDirectory: Path

    @BeforeEach
    internal fun setUp() {
      mockGradleRunner = mockk {
        every { withArguments(*anyVararg()) }.returns(this)
        every { withArguments(any<List<String>>()) }.returns(this)
      }
      mockBuildResult = mockk()
      mockProjectDirectory = mockk()

      val mockFile: File = mockk()
      every { mockFile.toPath() }.returns(mockProjectDirectory)
      every { mockGradleRunner.projectDir }.returns(mockFile)
    }

    @Test
    internal fun `when 'build()' is called the project directory is a part of the build result`() {
      every { mockGradleRunner.arguments }.returns(emptyList())
      every { mockGradleRunner.build() }.returns(mockBuildResult)

      val result = mockGradleRunner.build("task1", "task2")

      verifyOrder {
        mockGradleRunner.withArguments(listOf("task1", "task2"))
        mockGradleRunner.build()
      }
      expectThat(result.projectDir)
        .isEqualTo(mockProjectDirectory)
    }

    @Test
    internal fun `when 'build()' is called with arguments then those arguments are appended to the existing arguments`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      every { mockGradleRunner.build() }.returns(mockBuildResult)

      mockGradleRunner.build("task1", "task2")

      verifyOrder {
        mockGradleRunner.withArguments(original + listOf("task1", "task2"))
        mockGradleRunner.build()
      }
    }

    @Test
    internal fun `when 'build()' is called then the arguments are reset afterwards`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      every { mockGradleRunner.build() }.returns(mockBuildResult)

      mockGradleRunner.build("task1", "task2")

      verifyOrder {
        mockGradleRunner.build()
        mockGradleRunner.withArguments(original)
      }
    }

    @Test
    internal fun `when 'build()' throws an exception then the arguments are still restored`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      val exception = RuntimeException("test exception")
      every { mockGradleRunner.build() }.throws(exception)

      expectCatching { mockGradleRunner.build("task1", "task2") }
        .isFailure()
        .isEqualTo(exception)

      verifyOrder {
        mockGradleRunner.withArguments(original + listOf("task1", "task2"))
        mockGradleRunner.build()
        mockGradleRunner.withArguments(original)
      }
    }

    @Test
    internal fun `when 'buildAndFail()' is called with arguments then those arguments are appended to the existing arguments`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      every { mockGradleRunner.buildAndFail() }.returns(mockBuildResult)

      mockGradleRunner.buildAndFail("task1", "task2")

      verifyOrder {
        mockGradleRunner.withArguments(original + listOf("task1", "task2"))
        mockGradleRunner.buildAndFail()
        mockGradleRunner.withArguments(original)
      }
    }

    @Test
    internal fun `when 'buildAndFail()' is called then the arguments are reset afterwards`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      every { mockGradleRunner.buildAndFail() }.returns(mockBuildResult)

      mockGradleRunner.buildAndFail("task1", "task2")

      verifyOrder {
        mockGradleRunner.buildAndFail()
        mockGradleRunner.withArguments(original)
      }
    }

    @Test
    internal fun `when 'buildAndFail()' throws an exception then the arguments are still restored`() {
      val original = listOf("some", "args")
      every { mockGradleRunner.arguments }.returns(original)
      val exception = RuntimeException("test exception")
      every { mockGradleRunner.buildAndFail() }.throws(exception)

      Assertions.assertThatCode { mockGradleRunner.buildAndFail("task1", "task2") }
        .describedAs("Rethrows exception")
        .isEqualTo(exception)

      verifyOrder {
        mockGradleRunner.buildAndFail()
        mockGradleRunner.withArguments(original)
      }
    }

    @Test
    internal fun `when 'buildAndFail()' is called the project directory is a part of the build result`() {
      every { mockGradleRunner.arguments }.returns(emptyList())
      every { mockGradleRunner.buildAndFail() }.returns(mockBuildResult)

      val result = mockGradleRunner.buildAndFail("task1", "task2")

      verify {
        mockGradleRunner.buildAndFail()
      }
      expectThat(result.projectDir)
        .isEqualTo(mockProjectDirectory)
    }
  }

  @Nested
  internal inner class FsExtensionsTest {
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
  }

  @Test
  internal fun `when withSystemEnvironment is called then the environment is set to null`() {
    every { mockGradleRunner.withEnvironment(any()) }.returns(mockGradleRunner)

    expectCatching { mockGradleRunner.withSystemEnvironment() }
      .isSuccess()

    verify { mockGradleRunner.withEnvironment(null) }
  }

  @Test
  internal fun `when withEnvironment is called with an empty variadic of pairs, then the environment is set to an empty map`() {
    every { mockGradleRunner.withEnvironment(any()) }.returns(mockGradleRunner)

    expectCatching { mockGradleRunner.withEnvironment() }
      .isSuccess()

    verify { mockGradleRunner.withEnvironment(emptyMap()) }
  }

  @Test
  internal fun `when withEnvironment is called with pairs, then the environment is set to those values empty map`() {
    every { mockGradleRunner.withEnvironment(any()) }.returns(mockGradleRunner)

    expectCatching { mockGradleRunner.withEnvironment("a" to "b", "c" to "d") }
      .isSuccess()

    verify { mockGradleRunner.withEnvironment(mapOf("a" to "b", "c" to "d")) }
  }
}
