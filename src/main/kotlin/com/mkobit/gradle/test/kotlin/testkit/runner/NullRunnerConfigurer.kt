package com.mkobit.gradle.test.kotlin.testkit.runner

import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner

/**
 * Null object implementation of [RunnerConfigurer].
 */
// TODO: mkobit - can this be used with ServiceLoader?
object NullRunnerConfigurer : RunnerConfigurer {

  private val logger = KotlinLogging.logger {}

  override fun invoke(gradleRunner: GradleRunner) {
    logger.debug { "${NullRunnerConfigurer::class.java.simpleName} invoked on runner" }
  }
}
