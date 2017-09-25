package com.mkobit.gradle.test.testkit.runner

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import testsupport.BooleanSource
import java.io.File
import java.io.Writer
import java.net.URI
import java.nio.file.Paths

internal class GradleRunnerExtensionTest {

  private lateinit var mockRunnerConfigurer: RunnerConfigurer
  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var mockBuildResult: BuildResult

  @BeforeEach
  internal fun setUp() {
    mockRunnerConfigurer = mock()
    mockBuildResult = mock()
    mockGradleRunner = mock {
      on { build() } doReturn mockBuildResult
      on { buildAndFail() } doReturn mockBuildResult
    }
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
  internal fun `cannot resolve path when projectDir not set`() {
    whenever(mockGradleRunner.projectDir).thenReturn(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }

  @Test
  internal fun `build extension - no arguments provided`() {
    mockGradleRunner.buildWith(runnerConfigurer = mockRunnerConfigurer)

    verify(mockRunnerConfigurer, times(1)).invoke(mockGradleRunner)
    verify(mockGradleRunner, times(1)).build()
    verifyNoMoreInteractions(mockGradleRunner)
  }

  @Test
  internal fun `build extension - with projectDir`() {
    val mockFile: File = mock()
    mockGradleRunner.buildWith(projectDir = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withProjectDir(mockFile)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with args`() {
    val args = listOf<String>()
    mockGradleRunner.buildWith(arguments = args, runnerConfigurer = mockRunnerConfigurer)
    verify(mockGradleRunner, times(1)).withArguments(args)
    verify(mockGradleRunner, times(1)).build()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build extension - with debug`(boolean: Boolean) {
    mockGradleRunner.buildWith(debug = boolean, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withDebug(boolean)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with distribution`() {
    val mockUri = mock<URI>()
    mockGradleRunner.buildWith(distribution = mockUri, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleDistribution(mockUri)
    verify(mockGradleRunner, times(1)).build()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build extension - with forwardOutput`(boolean: Boolean) {
    mockGradleRunner.buildWith(forwardOutput = boolean, runnerConfigurer = mockRunnerConfigurer)

    if (boolean) {
      verify(mockGradleRunner, times(1)).forwardOutput()
    } else {
      verify(mockGradleRunner, never()).forwardOutput()
    }
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with installation`() {
    val mockFile = mock<File>()
    mockGradleRunner.buildWith(installation = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleInstallation(mockFile)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with versionNumber`() {
    val version = "version"
    mockGradleRunner.buildWith(versionNumber = version, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleVersion(version)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with forwardStdError`() {
    val mockWriter = mock<Writer>()
    mockGradleRunner.buildWith(forwardStdError = mockWriter, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).forwardStdError(mockWriter)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with forwardStdOutput`() {
    val mockWriter = mock<Writer>()
    mockGradleRunner.buildWith(forwardStdOutput = mockWriter, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).forwardStdOutput(mockWriter)
    verify(mockGradleRunner, times(1)).build()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build extension - with usePluginClasspath`(boolean: Boolean) {
    mockGradleRunner.buildWith(usePluginClasspath = boolean, runnerConfigurer = mockRunnerConfigurer)

    if (boolean) {
      verify(mockGradleRunner, times(1)).withPluginClasspath()
    } else {
      verify(mockGradleRunner, never()).withPluginClasspath()
    }
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with testKitDir`() {
    val mockFile = mock<File>()
    mockGradleRunner.buildWith(testKitDir = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withTestKitDir(mockFile)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with pluginClasspath`() {
    val mockFileIterator: Iterable<File> = mock()
    mockGradleRunner.buildWith(pluginClasspath = mockFileIterator, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withPluginClasspath(mockFileIterator)
    verify(mockGradleRunner, never()).withPluginClasspath()
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - with configuration`() {
    val mockRunnerConfigurer: RunnerConfigurer = mock()
    mockGradleRunner.buildWith(runnerConfigurer = mockRunnerConfigurer)

    verify(mockRunnerConfigurer, times(1)).invoke(mockGradleRunner)
    verify(mockGradleRunner, times(1)).build()
  }

  @Test
  internal fun `build extension - cannot use forwardOutput and a forwardStdOutput writer`() {
    val mockWriter: Writer = mock()
    assertThatIllegalArgumentException().isThrownBy {
      mockGradleRunner.buildWith(forwardOutput = true, forwardStdOutput = mockWriter)
    }
  }

  @Test
  internal fun `build extension - cannot use forwardOutput and a forwardStdError writer`() {
    val mockWriter: Writer = mock()
    assertThatIllegalArgumentException().isThrownBy {
      mockGradleRunner.buildWith(forwardOutput = true, forwardStdError = mockWriter)
    }
  }

  @Test
  internal fun `build and fail extension - no arguments provided`() {
    mockGradleRunner.buildAndFailWith(runnerConfigurer = mockRunnerConfigurer)

    verify(mockRunnerConfigurer, times(1)).invoke(mockGradleRunner)
    verify(mockGradleRunner, times(1)).buildAndFail()
    verifyNoMoreInteractions(mockGradleRunner)
  }

  @Test
  internal fun `build and fail extension - with projectDir`() {
    val mockFile: File = mock()
    mockGradleRunner.buildAndFailWith(projectDir = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withProjectDir(mockFile)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with args`() {
    val args = listOf<String>()
    mockGradleRunner.buildAndFailWith(arguments = args, runnerConfigurer = mockRunnerConfigurer)
    verify(mockGradleRunner, times(1)).withArguments(args)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build and fail extension - with debug`(boolean: Boolean) {
    mockGradleRunner.buildAndFailWith(debug = boolean, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withDebug(boolean)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with distribution`() {
    val mockUri = mock<URI>()
    mockGradleRunner.buildAndFailWith(distribution = mockUri, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleDistribution(mockUri)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build and fail extension - with forwardOutput`(boolean: Boolean) {
    mockGradleRunner.buildAndFailWith(forwardOutput = boolean, runnerConfigurer = mockRunnerConfigurer)

    if (boolean) {
      verify(mockGradleRunner, times(1)).forwardOutput()
    } else {
      verify(mockGradleRunner, never()).forwardOutput()
    }
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with installation`() {
    val mockFile = mock<File>()
    mockGradleRunner.buildAndFailWith(installation = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleInstallation(mockFile)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with versionNumber`() {
    val version = "version"
    mockGradleRunner.buildAndFailWith(versionNumber = version, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withGradleVersion(version)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with forwardStdError`() {
    val mockWriter = mock<Writer>()
    mockGradleRunner.buildAndFailWith(forwardStdError = mockWriter, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).forwardStdError(mockWriter)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with forwardStdOutput`() {
    val mockWriter = mock<Writer>()
    mockGradleRunner.buildAndFailWith(forwardStdOutput = mockWriter, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).forwardStdOutput(mockWriter)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @ParameterizedTest
  @BooleanSource
  internal fun `build and fail extension - with usePluginClasspath`(boolean: Boolean) {
    mockGradleRunner.buildAndFailWith(usePluginClasspath = boolean, runnerConfigurer = mockRunnerConfigurer)

    if (boolean) {
      verify(mockGradleRunner, times(1)).withPluginClasspath()
    } else {
      verify(mockGradleRunner, never()).withPluginClasspath()
    }
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with testKitDir`() {
    val mockFile = mock<File>()
    mockGradleRunner.buildAndFailWith(testKitDir = mockFile, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withTestKitDir(mockFile)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with pluginClasspath`() {
    val mockFileIterator: Iterable<File> = mock()
    mockGradleRunner.buildAndFailWith(pluginClasspath = mockFileIterator, runnerConfigurer = mockRunnerConfigurer)

    verify(mockGradleRunner, times(1)).withPluginClasspath(mockFileIterator)
    verify(mockGradleRunner, never()).withPluginClasspath()
    verify(mockGradleRunner, times(1)).buildAndFail()
  }

  @Test
  internal fun `build and fail extension - with configuration`() {
    mockGradleRunner.buildAndFailWith(runnerConfigurer = mockRunnerConfigurer)

    verify(mockRunnerConfigurer, times(1)).invoke(mockGradleRunner)
    verify(mockGradleRunner, times(1)).buildAndFail()
  }


  @Test
  internal fun `build and fail extension - cannot use forwardOutput and a forwardStdOutput writer`() {
    val mockWriter: Writer = mock()
    assertThatIllegalArgumentException().isThrownBy {
      mockGradleRunner.buildAndFailWith(forwardOutput = true, forwardStdOutput = mockWriter)
    }
  }

  @Test
  internal fun `build and fail extension - cannot use forwardOutput and a forwardStdError writer`() {
    val mockWriter: Writer = mock()
    assertThatIllegalArgumentException().isThrownBy {
      mockGradleRunner.buildAndFailWith(forwardOutput = true, forwardStdError = mockWriter)
    }
  }
}
