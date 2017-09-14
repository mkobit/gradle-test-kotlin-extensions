package com.mkobit.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Path

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

/**
 * Get the path resolved against the [GradleRunner.getProjectDir] path.
 * @throws IllegalStateException when [GradleRunner.getProjectDir] is `null`
 * @throws IllegalArgumentException when [other] is an absolute path
 */
fun GradleRunner.resolveFromProjectDir(other: Path): Path {
  require(!other.isAbsolute) { "Path $other must not be absolute" }
  val projectDirectory: File = projectDir ?: throw IllegalStateException("projectDir must not be null to resolve path")
  return projectDirectory.toPath().resolve(other)
}
