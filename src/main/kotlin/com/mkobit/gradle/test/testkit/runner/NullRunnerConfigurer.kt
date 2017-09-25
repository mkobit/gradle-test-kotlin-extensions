package com.mkobit.gradle.test.testkit.runner

import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner

object NullRunnerConfigurer : RunnerConfigurer {

  private val logger = KotlinLogging.logger {}

  override fun invoke(gradleRunner: GradleRunner) {
    logger.debug { "${NullRunnerConfigurer::class.java.simpleName} invoked on runner" }
  }
}
