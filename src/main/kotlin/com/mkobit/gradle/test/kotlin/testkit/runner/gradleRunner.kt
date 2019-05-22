package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Path

/**
 * Runs a build using [GradleRunner.build] by appending the [tasks] to the [GradleRunner.getArguments]. The original
 * [GradleRunner.getArguments] are restored after the build.
 * @param tasks the tasks to execute
 * @return the result of the build
 * @see GradleRunner.build
 */
public fun GradleRunner.build(tasks: Iterable<String>): KBuildResult {
  val originalArguments = arguments
  withArguments(originalArguments + tasks)
  try {
    val buildResult = build()
    // We know if we didn't bomb out yet, the build directory must be set
    return DefaultKBuildResult(projectDir?.toPath()!!, buildResult)
  } finally {
    withArguments(originalArguments)
  }
}

/**
 * Runs a build using [GradleRunner.build] by appending the [tasks] to the [GradleRunner.getArguments]. The original
 * [GradleRunner.getArguments] are restored after the build.
 * @param tasks the tasks to execute
 * @return the result of the build
 * @see GradleRunner.build
 */
public fun GradleRunner.build(vararg tasks: String): KBuildResult = build(tasks.toList())

/**
 * Runs a build that is expected to fail using [GradleRunner.buildAndFail] by appending the [tasks] to
 * the [GradleRunner.getArguments]. The original [GradleRunner.getArguments] are restored after the build.
 * @param tasks the tasks to execute
 * @return the result of the build
 * @see GradleRunner.buildAndFail
 */
public fun GradleRunner.buildAndFail(tasks: Collection<String>): KBuildResult {
  val originalArguments = arguments
  withArguments(originalArguments + tasks)
  try {
    val buildResult = buildAndFail()
    // We know if an exception hasn't been thrown yet from running the build, then the build directory must be set
    return DefaultKBuildResult(projectDir?.toPath()!!, buildResult)
  } finally {
    withArguments(originalArguments)
  }
}

/**
 * Runs a build that is expected to fail using [GradleRunner.buildAndFail] by appending the [tasks] to
 * the [GradleRunner.getArguments]. The original [GradleRunner.getArguments] are restored after the build.
 * @param tasks the tasks to execute
 * @return the result of the build
 * @see GradleRunner.buildAndFail
 */
public fun GradleRunner.buildAndFail(vararg tasks: String): KBuildResult = buildAndFail(tasks.toList())

/**
 * Get the path resolved against the [GradleRunner.getProjectDir] path.
 * @throws IllegalStateException when [GradleRunner.getProjectDir] is `null`
 * @throws IllegalArgumentException when [other] is an absolute path
 */
@Deprecated("Superfluous method, will be removed in a future release")
fun GradleRunner.resolveFromProjectDir(other: Path): Path {
  require(!other.isAbsolute) { "Path $other must not be absolute" }
  val projectDirectory: File = projectDir ?: throw IllegalStateException("projectDir must not be null to resolve path")
  return projectDirectory.toPath().resolve(other)
}

/**
 * Sets the directory that the Gradle build will be executed in.
 * @param projectDir the project directory
 * @see GradleRunner.withProjectDir
 */
public fun GradleRunner.withProjectDir(projectDir: Path): GradleRunner =
  withProjectDir(projectDir.toFile())

/**
 * Sets the runner environment variables to that of the system environment.
 * @see GradleRunner.withEnvironment
 */
public fun GradleRunner.withSystemEnvironment(): GradleRunner =
  withEnvironment(null)

/**
 * Sets the environment variables for the runner to the provided [environmentVariables].
 * @see GradleRunner.withEnvironment
 */
public fun GradleRunner.withEnvironment(vararg environmentVariables: Pair<String, String>): GradleRunner =
  withEnvironment(environmentVariables.toMap())
