package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

val BuildTask.success: Boolean
  get() = outcome == TaskOutcome.SUCCESS

val BuildTask.failed: Boolean
  get() = outcome == TaskOutcome.FAILED

val BuildTask.fromCache: Boolean
  get() = outcome == TaskOutcome.FROM_CACHE

val BuildTask.noSource: Boolean
  get() = outcome == TaskOutcome.NO_SOURCE

val BuildTask.skipped: Boolean
  get() = outcome == TaskOutcome.SKIPPED

val BuildTask.upToDate: Boolean
  get() = outcome == TaskOutcome.UP_TO_DATE
