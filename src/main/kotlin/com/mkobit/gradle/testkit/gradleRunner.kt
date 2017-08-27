package com.mkobit.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun build(projectDirectory: File, vararg arguments: String): BuildResult {
  return GradleRunner.create()
    .withPluginClasspath()
    .withProjectDir(projectDirectory)
    .withArguments(*arguments)
    .build()
}

fun buildAndFail(projectDirectory: File, vararg arguments: String): BuildResult {
  return GradleRunner.create()
    .withPluginClasspath()
    .withProjectDir(projectDirectory)
    .withArguments(*arguments)
    .buildAndFail()
}
