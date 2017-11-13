package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner

/*
 * Extensions for managing CLI options for the GradleRunner.
 */
// TODO: support short options for all of these, especially project properties

/**
 * The `--build-cache` flag.
 */
var GradleRunner.buildCacheEnabled: Boolean
  get() = arguments.contains("--build-cache")
  set(value) {
    ensureToggleableArgumentState("--build-cache", value)
  }

/**
 * The `--no-build-cache` flag.
 */
var GradleRunner.buildCacheDisabled: Boolean
  get() = arguments.contains("--no-build-cache")
  set(value) {
    ensureToggleableArgumentState("--no-build-cache", value)
  }

/**
 * The `--continue` flag.
 */
var GradleRunner.continueAfterFailure: Boolean
  get() = arguments.contains("--continue")
  set(value) {
    ensureToggleableArgumentState("--continue", value)
  }

/**
 * The `--quiet` flag.
 */
var GradleRunner.quiet: Boolean
  get() = arguments.contains("--quiet")
  set(value) {
    ensureToggleableArgumentState("--quiet", value)
  }

/**
 * The `--info` flag.
 */
var GradleRunner.info: Boolean
  get() = arguments.contains("--info")
  set(value) {
    ensureToggleableArgumentState("--info", value)
  }

/**
 * The `--debug` flag.
 */
var GradleRunner.debug: Boolean
  get() = arguments.contains("--debug")
  set(value) {
    ensureToggleableArgumentState("--debug", value)
  }

/**
 * The `--warn` flag.
 */
var GradleRunner.warn: Boolean
  get() = arguments.contains("--warn")
  set(value) {
    ensureToggleableArgumentState("--warn", value)
  }

/**
 * The `--init-script` options.
 */
var GradleRunner.initScripts: List<String>
  get() = arguments.findAllKeyValueArgumentValues { it == "--init-script" }
  set(value) {
    withArguments(
        arguments.filterOutKeyValueArguments { it == "--init-script" }
            + value.flatMap { listOf("--init-script", it) }
    )
  }

/**
 * The `--exclude-task` options.
 */
var GradleRunner.excludedTasks: List<String>
  get() = arguments.findAllKeyValueArgumentValues { it == "--exclude-task" }
  set(value) {
    withArguments(
        arguments.filterOutKeyValueArguments { it == "--exclude-task" }
            + value.flatMap { listOf("--exclude-task", it) }
    )
  }

/**
 * The `--scan` flag.
 */
var GradleRunner.buildScanEnabled: Boolean
  get() = arguments.contains("--scan")
  set(value) {
    ensureToggleableArgumentState("--scan", value)
  }

/**
 * The `--no-scan` flag.
 */
var GradleRunner.buildScanDisabled: Boolean
  get() = arguments.contains("--no-scan")
  set(value) {
    ensureToggleableArgumentState("--no-scan", value)
  }

var GradleRunner.offline: Boolean
  get() = arguments.contains("--offline")
  set(value) {
    ensureToggleableArgumentState("--offline", value)
  }

private val projectPropertySplitPattern = Regex("=")
/**
 * The `--project-prop` options.
 */
var GradleRunner.projectProperties: Map<String, String?>
  get() = arguments
      .findAllKeyValueArgumentValues { it == "--project-prop" }
      .map { it.split(projectPropertySplitPattern, 2) }
      .associateBy({ it.first() }, { it.getOrNull(1) })
  set(value) {
    val properties = value.flatMap { (key, value) ->
      listOf("--project-prop", "$key${value?.let { "=$it" }.orEmpty()}")
    }
    withArguments(arguments.filterOutKeyValueArguments { it == "--project-prop" } + properties)
  }

/**
 * Updates the [GradleRunner.getArguments] to ensure that the provided [argument] is included or excluded
 * depending on the value of the [include].
 * @param argument the argument to ensure is present in the [GradleRunner.getArguments]
 * @param include `true` if the [argument] should be included, `false` is not
 */
private fun GradleRunner.ensureToggleableArgumentState(argument: String, include: Boolean) {
  val currentlyContained = arguments.contains(argument)
  if (include) {
    if (!currentlyContained) {
      withArguments(arguments + listOf(argument))
    }
  } else {
    if (currentlyContained) {
      withArguments(arguments.filter { it != argument })
    }
  }
}

/**
 * Finds all key/value spaces arguments values based on the provided [argumentPredicate].
 * For example, if the command is `["--help", "--arg1", "val1"]` and the predicate is `{ it == "--arg1" }`
 * then the output will be `["val1"]`.
 */
private fun List<String>.findAllKeyValueArgumentValues(
    argumentPredicate: (key: String) -> Boolean
): List<String> {
  var lastArgumentTest = false
  return filter {
    if (lastArgumentTest) {
      lastArgumentTest = false
      true
    } else {
      lastArgumentTest = argumentPredicate(it)
      false
    }
  }
}

/**
 * Finds all key/value spaces arguments based on the provided [argumentPredicate].
 * For example, if the command is `["--help", "--arg1", "val1"]` and the predicate is `{ it == "--arg1" }`
 * then the output will be `["--help"]`.
 */
private fun List<String>.filterOutKeyValueArguments(
    argumentPredicate: (key: String) -> Boolean
): List<String> {
  var lastArgumentTest = false
  return filterNot {
    if (lastArgumentTest) {
      lastArgumentTest = false
      true
    } else {
      if (argumentPredicate(it)) {
        lastArgumentTest = true
        true
      } else {
        false
      }
    }
  }
}
