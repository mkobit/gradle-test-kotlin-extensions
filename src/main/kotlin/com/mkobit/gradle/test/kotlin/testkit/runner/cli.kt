package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import java.nio.file.Paths

/*
 * Extensions for managing CLI options for the GradleRunner.
 */
// TODO: support short options for all of these, especially project properties

/**
 * The `--build-file` option. Setting to `null` removes the option and value.
 */
public var GradleRunner.buildFile: Path?
  get() = findOptionValue("--build-file")?.let { Paths.get(it) }
  set(value) {
    ensureOptionHasValue("--build-file", value)
  }

/**
 * The `--project-cache-dir` option. Setting to `null` removes the option and value.
 */
public var GradleRunner.projectCacheDir: Path?
  get() = findOptionValue("--project-cache-dir")?.let { Paths.get(it) }
  set(value) {
    ensureOptionHasValue("--project-cache-dir", value)
  }

@Deprecated("Renamed to buildCache", ReplaceWith("buildCache", "com.mkobit.gradle.test.kotlin.testkit.runner.buildCache"), DeprecationLevel.ERROR)
public var GradleRunner.buildCacheEnabled: Boolean
  get() = buildCache
  set(value) {
    buildCache = value
  }

/**
 * The `--build-cache` flag.
 */
public var GradleRunner.buildCache: Boolean
  get() = arguments.contains("--build-cache")
  set(value) {
    ensureFlagOptionState("--build-cache", value)
  }

/**
 * The `--no-build-cache` flag.
 */
public var GradleRunner.noBuildCache: Boolean
  get() = arguments.contains("--no-build-cache")
  set(value) {
    ensureFlagOptionState("--no-build-cache", value)
  }

@Deprecated("Renamed to noBuildScan", ReplaceWith("noBuildCache", "com.mkobit.gradle.test.kotlin.testkit.runner.noBuildCache"), DeprecationLevel.ERROR)
public var GradleRunner.buildCacheDisabled: Boolean
  get() = noBuildCache
  set(value) {
    noBuildCache = value
  }

/**
 * The `--configure-on-demand` flag.
 */
public var GradleRunner.configureOnDemand: Boolean
  get() = arguments.contains("--configure-on-demand")
  set(value) {
    ensureFlagOptionState("--configure-on-demand", value)
  }

/**
 * The `--continue` flag.
 */
public var GradleRunner.continueAfterFailure: Boolean
  get() = arguments.contains("--continue")
  set(value) {
    ensureFlagOptionState("--continue", value)
  }

private val systemPropertySplitPattern = Regex("=")
/**
 * The `--system-prop` properties.
 */
public var GradleRunner.systemProperties: Map<String, String?>
  get() = findRepeatableOptionValues { it == "--system-prop" }
    .map { it.split(systemPropertySplitPattern, 2) }
    .associateBy({ it.first() }, { it.getOrNull(1) })
  set(value) {
    // TODO: check for empty keys
    val properties = value.map { (key, value) ->
      "$key${value?.let { "=$it" }.orEmpty()}"
    }
    ensureRepeatableOptionHasValues("--system-prop", properties)
  }

/**
 * The `--quiet` flag.
 */
public var GradleRunner.quiet: Boolean
  get() = arguments.contains("--quiet")
  set(value) {
    ensureFlagOptionState("--quiet", value)
  }

/**
 * The `--stacktrace` flag.
 */
public var GradleRunner.stacktrace: Boolean
  get() = arguments.contains("--stacktrace")
  set(value) {
    ensureFlagOptionState("--stacktrace", value)
  }

/**
 * The `--full-stacktrace` flag.
 */
public var GradleRunner.fullStacktrace: Boolean
  get() = arguments.contains("--full-stacktrace")
  set(value) {
    ensureFlagOptionState("--full-stacktrace", value)
  }

/**
 * The `--info` flag.
 */
public var GradleRunner.info: Boolean
  get() = arguments.contains("--info")
  set(value) {
    ensureFlagOptionState("--info", value)
  }

/**
 * The `--dry-run` flag.
 */
public var GradleRunner.dryRun: Boolean
  get() = arguments.contains("--dry-run")
  set(value) {
    ensureFlagOptionState("--dry-run", value)
  }

/**
 * The `--debug` flag.
 */
public var GradleRunner.debug: Boolean
  get() = arguments.contains("--debug")
  set(value) {
    ensureFlagOptionState("--debug", value)
  }

/**
 * The `--warn` flag.
 */
public var GradleRunner.warn: Boolean
  get() = arguments.contains("--warn")
  set(value) {
    ensureFlagOptionState("--warn", value)
  }

/**
 * The `--init-script` options.
 */
public var GradleRunner.initScripts: List<Path>
  get() = findRepeatableOptionValues { it == "--init-script" }.map { Paths.get(it) }
  set(value) {
    ensureRepeatableOptionHasValues("--init-script", value)
  }

/**
 * The `--include-build` options.
 */
public var GradleRunner.includedBuilds: List<Path>
  get() = findRepeatableOptionValues { it == "--include-build" }.map { Paths.get(it) }
  set(value) {
    ensureRepeatableOptionHasValues("--include-build", value)
  }

/**
 * The `--exclude-task` options.
 */
public var GradleRunner.excludedTasks: List<String>
  get() = findRepeatableOptionValues { it == "--exclude-task" }
  set(value) {
    ensureRepeatableOptionHasValues("--exclude-task", value)
  }

/**
 * The `--scan` flag.
 */
public var GradleRunner.buildScan: Boolean
  get() = arguments.contains("--scan")
  set(value) {
    ensureFlagOptionState("--scan", value)
  }

@Deprecated("Renamed to buildScan", ReplaceWith("buildScan", "com.mkobit.gradle.test.kotlin.testkit.runner.buildScan"), DeprecationLevel.ERROR)
public var GradleRunner.buildScanEnabled: Boolean
  get() = buildScan
  set(value) {
    buildScan = value
  }

/**
 * The `--no-scan` flag.
 */
public var GradleRunner.noBuildScan: Boolean
  get() = arguments.contains("--no-scan")
  set(value) {
    ensureFlagOptionState("--no-scan", value)
  }

@Deprecated("Renamed to noBuildScan", ReplaceWith("noBuildScan", "com.mkobit.gradle.test.kotlin.testkit.runner.noBuildScan"), DeprecationLevel.ERROR)
public var GradleRunner.buildScanDisabled: Boolean
  get() = noBuildScan
  set(value) {
    noBuildScan = value
  }

/**
 * The `--max-workers` option.
 */
public var GradleRunner.maxWorkers: Int?
  get() = findOptionValue("--max-workers")?.toInt()
  set(value) {
    if (value != null) {
      require(value >= 0) { "Max workers must be a non-negative integer" }
    }
    ensureOptionHasValue("--max-workers", value)
  }

/**
 * The `--offline` flag.
 */
public var GradleRunner.offline: Boolean
  get() = arguments.contains("--offline")
  set(value) {
    ensureFlagOptionState("--offline", value)
  }

private val projectPropertySplitPattern = Regex("=")
/**
 * The `--project-prop` properties.
 */
public var GradleRunner.projectProperties: Map<String, String?>
  get() = findRepeatableOptionValues { it == "--project-prop" }
    .map { it.split(projectPropertySplitPattern, 2) }
    .associateBy({ it.first() }, { it.getOrNull(1) })
  set(value) {
    // TODO: make sure keys can't be empty
    val properties = value.map { (key, value) ->
      "$key${value?.let { "=$it" }.orEmpty()}"
    }
    ensureRepeatableOptionHasValues("--project-prop", properties)
  }

/**
 * The `--no-parallel` flag.
 */
public var GradleRunner.noParallel: Boolean
  get() = arguments.contains("--no-parallel")
  set(value) {
    ensureFlagOptionState("--no-parallel", value)
  }

/**
 * The `--no-parallel` flag.
 */
public var GradleRunner.parallel: Boolean
  get() = arguments.contains("--parallel")
  set(value) {
    ensureFlagOptionState("--parallel", value)
  }

/**
 * The `--profile` flag.
 */
public var GradleRunner.profile: Boolean
  get() = arguments.contains("--profile")
  set(value) {
    ensureFlagOptionState("--profile", value)
  }

/**
 * The `--refresh-dependencies` flag.
 */
public var GradleRunner.refreshDependencies: Boolean
  get() = arguments.contains("--refresh-dependencies")
  set(value) {
    ensureFlagOptionState("--refresh-dependencies", value)
  }

/**
 * The `--rerun-tasks` flag.
 */
public var GradleRunner.rerunTasks: Boolean
  get() = arguments.contains("--rerun-tasks")
  set(value) {
    ensureFlagOptionState("--rerun-tasks", value)
  }

/**
 * The `--settings-file` option. Setting to `null` removes the option and value.
 */
public var GradleRunner.settingsFile: Path?
  get() = findOptionValue("--settings-file")?.let { Paths.get(it) }
  set(value) {
    ensureOptionHasValue("--settings-file", value)
  }

/**
 * Updates the [GradleRunner.getArguments] to ensure that the provided [flag] is included or excluded
 * depending on the value of the [include]. The flag should be a flag like `--enable-thing`.
 * @param flag the flag to ensure is present in the [GradleRunner.getArguments]
 * @param include `true` if the [flag] should be included, `false` if it should be removed
 */
private fun GradleRunner.ensureFlagOptionState(flag: String, include: Boolean) {
  val currentlyContained = arguments.contains(flag)
  if (include) {
    if (!currentlyContained) {
      withArguments(arguments + listOf(flag))
    }
  } else {
    if (currentlyContained) {
      withArguments(arguments.filter { it != flag })
    }
  }
}

/**
 * Ensures that a repeatable options in the command line are the provided [values]. When empty, they are removed.
 * @param option the option, for example `--init-script`.
 * @param values the values for the [option]
 */
private fun GradleRunner.ensureRepeatableOptionHasValues(option: String, values: List<Any>) {
  withArguments(
    filterKeyValueArgumentsFilteringOutOption { it == option } + values.flatMap { listOf(option, it.toString()) }
  )
}

/**
 * Ensures that the option has the specified [value]. If the [value] is `null`, then the option is removed.
 * @param option the option, for example `--build-file`
 * @param value the value of the option. When `null`, this means that the option should be removed
 */
private fun GradleRunner.ensureOptionHasValue(option: String, value: Any?) {
  val newOptionValue = if (value == null) {
    emptyList()
  } else {
    listOf(option, value.toString())
  }
  val optionIndex = arguments.indexOf(option)
  val newArguments = if (optionIndex == -1) {
    arguments + newOptionValue
  } else {
    arguments.filterIndexed { index, _ -> index !in setOf(optionIndex, optionIndex + 1) } + newOptionValue
  }
  withArguments(newArguments)
}

/**
 * Finds the value for an option in the arguments, if it present.
 * @param option the option to locate the value for, for example `--settings-file`
 * @return the value of the option, or `null` if it is not present
 */
private fun GradleRunner.findOptionValue(option: String): String? {
  var lastArgumentTest = false
  return arguments.find {
    if (lastArgumentTest) {
      true
    } else if (it == option) {
      lastArgumentTest = true
      false
    } else {
      false
    }
  }
}

/**
 * Finds all key/value spaces arguments values based on the provided [argumentPredicate].
 * For example, if the command is `["--help", "--arg1", "val1"]` and the predicate is `{ it == "--arg1" }`
 * then the output will be `["val1"]`.
 */
private fun GradleRunner.findRepeatableOptionValues(
  argumentPredicate: (key: String) -> Boolean
): List<String> {
  var lastArgumentTest = false
  return arguments.filter {
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
private fun GradleRunner.filterKeyValueArgumentsFilteringOutOption(
  argumentPredicate: (key: String) -> Boolean
): List<String> {
  var lastArgumentTest = false
  return arguments.filterNot {
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
