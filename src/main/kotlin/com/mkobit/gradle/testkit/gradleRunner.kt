package com.mkobit.gradle.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.io.Writer
import java.net.URI
import java.nio.file.Path

fun GradleRunner.buildWith(
  projectDir: File? = null,
  arguments: List<String>? = null,
  debug: Boolean? = null,
  distribution: URI? = null,
  forwardOutput: Boolean? = null,
  installation: File? = null,
  versionNumber: String? = null,
  forwardStdError: Writer? = null,
  forwardStdOutput: Writer? = null,
  usePluginClasspath: Boolean = true,
  testKitDir: File? = null,
  pluginClasspath: Iterable<File>? = null,
  configuration: GradleRunner.() -> Unit = {}
): BuildResult {
  require(!(forwardOutput == true && forwardStdOutput != null)) { "Cannot specify both forwardOutput and forwardStdOutput" }
  require(!(forwardOutput == true && forwardStdError != null)) { "Cannot specify both forwardOutput and forwardStdError" }
  return this.run {
    arguments?.let { withArguments(it) }
    debug?.let { withDebug(it) }

    if (forwardOutput == true) {
      forwardOutput()
    }
    forwardStdError?.let { forwardStdError(it) }
    forwardStdOutput?.let { forwardStdOutput(it) }
    distribution?.let { withGradleDistribution(it) }
    installation?.let { withGradleInstallation(it) }
    versionNumber?.let { withGradleVersion(it) }
    // pluginClasspath takes precendence over usePluginClasspath
    pluginClasspath?.let { withPluginClasspath(pluginClasspath) }
    if (usePluginClasspath && pluginClasspath == null) {
      withPluginClasspath()
    }
    projectDir?.let { withProjectDir(it) }
    testKitDir?.let { withTestKitDir(it) }

    configuration()
    build()
  }
}

fun GradleRunner.buildAndFailWith(
  projectDir: File? = null,
  arguments: List<String>? = null,
  debug: Boolean? = null,
  distribution: URI? = null,
  forwardOutput: Boolean? = null,
  installation: File? = null,
  versionNumber: String? = null,
  forwardStdError: Writer? = null,
  forwardStdOutput: Writer? = null,
  usePluginClasspath: Boolean = true,
  testKitDir: File? = null,
  pluginClasspath: Iterable<File>? = null,
  configuration: GradleRunner.() -> Unit = {}
): BuildResult {
  require(!(forwardOutput == true && forwardStdOutput != null)) { "Cannot specify both forwardOutput and forwardStdOutput" }
  require(!(forwardOutput == true && forwardStdError != null)) { "Cannot specify both forwardOutput and forwardStdError" }
  return this.run {
    arguments?.let { withArguments(it) }
    debug?.let { withDebug(it) }

    if (forwardOutput == true) {
      forwardOutput()
    }
    forwardStdError?.let { forwardStdError(it) }
    forwardStdOutput?.let { forwardStdOutput(it) }
    distribution?.let { withGradleDistribution(it) }
    installation?.let { withGradleInstallation(it) }
    versionNumber?.let { withGradleVersion(it) }
    // pluginClasspath takes precendence over usePluginClasspath
    pluginClasspath?.let { withPluginClasspath(pluginClasspath) }
    if (usePluginClasspath && pluginClasspath == null) {
      withPluginClasspath()
    }
    projectDir?.let { withProjectDir(it) }
    testKitDir?.let { withTestKitDir(it) }

    configuration()
    buildAndFail()
  }
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
