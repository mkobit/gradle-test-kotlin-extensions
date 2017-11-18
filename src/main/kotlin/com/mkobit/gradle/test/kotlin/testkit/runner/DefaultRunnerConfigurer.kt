package com.mkobit.gradle.test.kotlin.testkit.runner

import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner

/**
 * The default [RunnerConfigurer].
 *
 * Sets up additional context for the [GradleRunner] based on system properties.
 */
// VisibleForTesting - used to enable SystemProperty testing rather than mutating the actual system properties
@Deprecated("Use the GradleRunner extensions instead")
class DefaultRunnerConfigurer internal constructor(
  private val propertyLoader: PropertyLoader
): RunnerConfigurer {

  constructor() : this(SystemPropertyLoader())

  companion object {
    private val KEY_PREFIX = DefaultRunnerConfigurer::class.java.canonicalName
    private val logger = KotlinLogging.logger {}
  }

  override fun invoke(gradleRunner: GradleRunner) {
    val updatedArguments = gradleRunner.arguments.toMutableList()
    when (propertyLoader.getProperty(runnerKey("stacktrace"))) {
      "stacktrace" -> updatedArguments.add("--stacktrace")
      "full-stacktrace" -> updatedArguments.add("--full-stacktrace")
    }
    when (propertyLoader.getProperty(runnerKey("logLevel"))) {
      "quiet" -> updatedArguments.add("--quiet")
      "warn" -> updatedArguments.add("--warn")
      "info" -> updatedArguments.add("--info")
      "debug" -> updatedArguments.add("--debug")
    }
    if (updatedArguments != gradleRunner.arguments) {
      logger.debug { "Updating old arguments from ${gradleRunner.arguments} to $updatedArguments" }
      gradleRunner.withArguments(updatedArguments)
    } else {
      logger.debug { "No updates needed to GradleRunner arguments" }
    }

    // This configurer is expected to be used with Gradle's built-in 'java-gradle-plugin'
    gradleRunner.withPluginClasspath()
  }

  private fun runnerKey(keyName: String) = "$KEY_PREFIX.$keyName"
}
