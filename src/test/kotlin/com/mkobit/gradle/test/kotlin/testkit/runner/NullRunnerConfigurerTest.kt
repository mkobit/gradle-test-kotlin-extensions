package com.mkobit.gradle.test.kotlin.testkit.runner

import com.mkobit.gradle.test.kotlin.testkit.runner.NullRunnerConfigurer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test

internal class NullRunnerConfigurerTest {
  @Test
  internal fun `does not mutate the GradleRunner`() {
    val mockRunner: GradleRunner = mock()
    NullRunnerConfigurer.invoke(mockRunner)

    verifyZeroInteractions(mockRunner)
  }
}
