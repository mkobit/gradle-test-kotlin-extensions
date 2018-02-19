package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildTask

internal class DefaultKBuildTask(private val delegate: BuildTask) : KBuildTask, BuildTask by delegate
