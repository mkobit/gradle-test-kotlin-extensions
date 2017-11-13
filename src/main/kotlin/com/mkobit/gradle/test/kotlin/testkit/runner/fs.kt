package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path


/**
 * The [GradleRunner.getProjectDir] as a [Path].
 */
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) { withProjectDir(value?.toFile()) }
