package com.mkobit.gradle.test.testkit.runner

import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner
import java.util.ServiceLoader

/**
 * Loads available [RunnerConfigurer] via the [ServiceLoader] and executes them against the
 * invoked [GradleRunner].
 * If no services are located with the [ServiceLoader] then the [DefaultRunnerConfigurer] is used.
 */
class ServiceLoaderRunnerConfigurer : RunnerConfigurer {

  private val services: ServiceLoader<RunnerConfigurer> by lazy {
    ServiceLoader.load(RunnerConfigurer::class.java)
  }

  companion object {
    private val defaultConfigurer = DefaultRunnerConfigurer()
    private val logger = KotlinLogging.logger {}
  }

  override fun invoke(gradleRunner: GradleRunner) {
    val runnerConfigurers = services.toList()
    if (runnerConfigurers.isEmpty()) {
      logger.debug { "No configurers found, using default" }
      defaultConfigurer(gradleRunner)
    } else {
      // TODO: provide ordering semantics
      runnerConfigurers.onEach {
        logger.debug { "Running configurer $it" }
      }.forEach { configurer ->
        configurer(gradleRunner)
      }
    }
  }
}
