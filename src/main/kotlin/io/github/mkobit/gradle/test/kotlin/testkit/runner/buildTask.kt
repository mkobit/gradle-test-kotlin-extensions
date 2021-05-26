package io.github.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

/**
 * `true` if this task was [TaskOutcome.SUCCESS], otherwise `false`.
 */
val BuildTask.success: Boolean
  get() = outcome == TaskOutcome.SUCCESS

/**
 * `true` if this task was [TaskOutcome.FAILED], otherwise `false`.
 */
val BuildTask.failed: Boolean
  get() = outcome == TaskOutcome.FAILED

/**
 * `true` if this task was [TaskOutcome.FROM_CACHE], otherwise `false`.
 */
val BuildTask.fromCache: Boolean
  get() = outcome == TaskOutcome.FROM_CACHE

/**
 * `true` if this task was [org.gradle.testkit.runner.TaskOutcome.NO_SOURCE], otherwise `false`.
 */
val BuildTask.noSource: Boolean
  get() = outcome == TaskOutcome.NO_SOURCE

/**
 * `true` if this task was [TaskOutcome.SKIPPED], otherwise `false`.
 */
val BuildTask.skipped: Boolean
  get() = outcome == TaskOutcome.SKIPPED

/**
 * `true` if this task was [TaskOutcome.UP_TO_DATE], otherwise `false`.
 */
val BuildTask.upToDate: Boolean
  get() = outcome == TaskOutcome.UP_TO_DATE
