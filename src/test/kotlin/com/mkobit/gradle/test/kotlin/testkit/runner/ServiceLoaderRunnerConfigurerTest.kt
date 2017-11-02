package com.mkobit.gradle.test.kotlin.testkit.runner

import com.mkobit.gradle.test.kotlin.testkit.runner.ServiceLoaderRunnerConfigurer
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class ServiceLoaderRunnerConfigurerTest {

  private lateinit var mockGradleRunner: GradleRunner
  private lateinit var runnerConfigurer: ServiceLoaderRunnerConfigurer

  @BeforeEach
  internal fun setUp() {
    mockGradleRunner = mock()
    runnerConfigurer = ServiceLoaderRunnerConfigurer()
  }

  @Test
  internal fun `no registered services for ServiceLoader`() {
    assertThatCode { runnerConfigurer(mockGradleRunner) }.doesNotThrowAnyException()
    verify(mockGradleRunner, atLeastOnce()).arguments
  }

  @Disabled
  @Test
  internal fun `services registered for ServiceLoader`() {
    TODO("not implemented")
  }
}
