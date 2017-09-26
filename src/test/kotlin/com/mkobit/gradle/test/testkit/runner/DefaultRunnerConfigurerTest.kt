package com.mkobit.gradle.test.testkit.runner

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.anyVararg
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultRunnerConfigurerTest {

  private lateinit var mockPropertyLoader: PropertyLoader
  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var defaultRunnerConfigurer: DefaultRunnerConfigurer

  @BeforeEach
  internal fun setUp() {
    mockPropertyLoader = mock()
    mockGradleRunner = mock()
    defaultRunnerConfigurer = DefaultRunnerConfigurer(mockPropertyLoader)
  }

  @Test
  internal fun `no properties are enabled then no changes are applied to the GradleRunner`() {
    defaultRunnerConfigurer(mockGradleRunner)

    verify(mockGradleRunner, atMost(1)).withPluginClasspath()
    verify(mockGradleRunner, atLeastOnce()).arguments
    verify(mockGradleRunner, never()).withArguments(any<List<String>>())
    verify(mockGradleRunner, never()).withArguments(anyVararg<String>())
    verifyNoMoreInteractions(mockGradleRunner)
  }

  @MethodSource("stacktraceOptions")
  @ParameterizedTest(name = "{0} then {1} is passed into arguments")
  internal fun `stacktrace property is set to`(configurationValue: String, expectedOption: String) {
    whenever(mockPropertyLoader.getProperty(eq("com.mkobit.gradle.test.testkit.runner.DefaultRunnerConfigurer.stacktrace"), anyOrNull()))
      .thenReturn(configurationValue)
    whenever(mockGradleRunner.arguments).thenReturn(emptyList())

    defaultRunnerConfigurer(mockGradleRunner)

    verify(mockGradleRunner, never()).withArguments(anyVararg<String>())
    argumentCaptor<List<String>>().apply {
      verify(mockGradleRunner).withArguments(capture())
      assertThat(firstValue).hasOnlyOneElementSatisfying {
        assertThat(it).isEqualTo(expectedOption)
      }
    }
  }

  @Suppress("UNUSED")
  private fun stacktraceOptions(): Stream<Arguments> = Stream.of(
    Arguments.of("stacktrace", "--stacktrace"),
    Arguments.of("full-stacktrace", "--full-stacktrace")
  )

  @Test
  internal fun `adds new arguments to existing arguments`() {
    val originalArguments = listOf("clean", "assemble", "--scan")
    whenever(mockGradleRunner.arguments).thenReturn(originalArguments)

    // TODO: mkobit - figure out better way to test this without knowing this property
    whenever(mockPropertyLoader.getProperty(eq("com.mkobit.gradle.test.testkit.runner.DefaultRunnerConfigurer.stacktrace"), anyOrNull()))
      .thenReturn("stacktrace")

    defaultRunnerConfigurer(mockGradleRunner)

    argumentCaptor<List<String>>().apply {
      verify(mockGradleRunner).withArguments(capture())
      assertThat(firstValue).contains("--stacktrace")
        .containsAll(originalArguments)
        .hasSize(4)
    }
  }

  @MethodSource("logLevelOptions")
  @ParameterizedTest(name = "{0} then {1} is passed into arguments")
  internal fun `logLevel property is set to`(configurationValue: String, expectedOption: String) {
    whenever(mockPropertyLoader.getProperty(eq("com.mkobit.gradle.test.testkit.runner.DefaultRunnerConfigurer.logLevel"), anyOrNull()))
    .thenReturn(configurationValue)
    whenever(mockGradleRunner.arguments).thenReturn(emptyList())

    defaultRunnerConfigurer(mockGradleRunner)

    verify(mockGradleRunner, never()).withArguments(anyVararg<String>())

    argumentCaptor<List<String>>().apply {
      verify(mockGradleRunner).withArguments(capture())
      assertThat(firstValue).hasOnlyOneElementSatisfying {
        assertThat(it).isEqualTo(expectedOption)
      }
    }
  }

  @Suppress("UNUSED")
  private fun logLevelOptions(): Stream<Arguments> = Stream.of(
    Arguments.of("quiet", "--quiet"),
    Arguments.of("warn", "--warn"),
    Arguments.of("info", "--info"),
    Arguments.of("debug", "--debug")
  )

  @Test
  internal fun `withPluginClasspath() is called for use with Gradle's built in 'java-gradle-plugin'`() {
    whenever(mockGradleRunner.arguments).thenReturn(emptyList())

    defaultRunnerConfigurer(mockGradleRunner)

    verify(mockGradleRunner, times(1)).withPluginClasspath()
    verify(mockGradleRunner, never()).withPluginClasspath(any())
  }
}
