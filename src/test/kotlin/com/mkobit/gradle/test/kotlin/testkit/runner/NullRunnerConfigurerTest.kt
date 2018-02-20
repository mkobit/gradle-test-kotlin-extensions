package com.mkobit.gradle.test.kotlin.testkit.runner

import com.mkobit.gradle.test.kotlin.testkit.runner.NullRunnerConfigurer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

internal class NullRunnerConfigurerTest {
  @DisabledIfEnvironmentVariable(named = "CIRCLE_JOB", matches = "^test-java-9$")
  @Test
  internal fun `does not mutate the GradleRunner`() {
    val mockRunner: GradleRunner = mock()
    NullRunnerConfigurer.invoke(mockRunner)

    verifyZeroInteractions(mockRunner)
  }
}
