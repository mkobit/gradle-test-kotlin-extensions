package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildTask

/**
 * Default implementation of [KBuildTask].
 * @property delegate the actual build task
 */
internal class DefaultKBuildTask(private val delegate: BuildTask) : KBuildTask, BuildTask by delegate {
  override fun toString(): String = "DefaultKBuildTask(path=${delegate.path}, outcome=${delegate.outcome})"
}
