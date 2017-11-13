package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.io.Writer
import java.net.URI
import java.nio.file.Path

/**
 * The [GradleRunner.getProjectDir] as a [Path].
 */
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) { withProjectDir(value?.toFile()) }

/**
 * Configure and execute a [GradleRunner.build].
 * @param projectDir if not `null`, sets the project directory - see [GradleRunner.withProjectDir]
 * @param arguments if not `null`, sets the argument to pass to the build - see [GradleRunner.withArguments]
 * @param debug - if not `null`, runs the build in process so it is debuggable - see [GradleRunner.withDebug]
 * @param distribution - if not `null`, use the specified distribution - see [GradleRunner.withGradleDistribution]
 * @param forwardOutput - if not `null`, forwards the output to the running processes standard out - see [GradleRunner.forwardOutput]
 * @param installation - if not `null`, use the specified installation - see [GradleRunner.withGradleInstallation]
 * @param versionNumber - if not `null`, use the specified version from Gradle's servers - see [GradleRunner.withGradleVersion]
 * @param forwardStdError- if not `null`, forwards build process `stdErr` to the supplied `Writer` - see [GradleRunner.forwardStdError]
 * @param forwardStdOutput- if not `null`, forwards build process `stdOut` to the supplied `Writer` - see [GradleRunner.forwardStdOutput]
 * @param usePluginClasspath- if `true`, uses the plugin classpath provided by the `java-gradle-plugin`. See [GradleRunner.withPluginClasspath].
 * @param testKitDir if not `null`, use the specified directory for TestKit storage needs - see [GradleRunner.withTestKitDir]
 * @param pluginClasspath if not `null`, sets the classpath files to use - see [GradleRunner.withPluginClasspath]
 * @param runnerConfigurer the additional configuration to apply to the `GradleRunner` before execution
 * @throws IllegalArgumentException when [forwardOutput] is `true` and [forwardStdOutput] is specified
 * @throws IllegalArgumentException when [forwardOutput] is `true` and [forwardStdError] is specified
 * @throws IllegalArgumentException when [usePluginClasspath] is `true` and [pluginClasspath] is specified
 */
@Deprecated(message = "Scheduled for removal", replaceWith = ReplaceWith("GradleRunner.setup"))
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
  usePluginClasspath: Boolean? = null,
  testKitDir: File? = null,
  pluginClasspath: Iterable<File>? = null,
  runnerConfigurer: RunnerConfigurer = ServiceLoaderRunnerConfigurer()
): BuildResult {
  require(!(forwardOutput == true && forwardStdOutput != null)) { "Cannot specify both forwardOutput and forwardStdOutput" }
  require(!(forwardOutput == true && forwardStdError != null)) { "Cannot specify both forwardOutput and forwardStdError" }
  require(!(usePluginClasspath == true && pluginClasspath != null)) { "Cannot specify both usePluginClasspath and forwardStdError" }
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
  pluginClasspath?.let { withPluginClasspath(pluginClasspath) }
  if (usePluginClasspath == true) {
    withPluginClasspath()
  }
  projectDir?.let { withProjectDir(it) }
  testKitDir?.let { withTestKitDir(it) }

  runnerConfigurer(this)
  return build()
}

/**
 * Configure and execute a [GradleRunner.buildAndFail].
 * @param projectDir if not `null`, sets the project directory - see [GradleRunner.withProjectDir]
 * @param arguments if not `null`, sets the argument to pass to the build - see [GradleRunner.withArguments]
 * @param debug - if not `null`, runs the build in process so it is debuggable - see [GradleRunner.withDebug]
 * @param distribution - if not `null`, use the specified distribution - see [GradleRunner.withGradleDistribution]
 * @param forwardOutput - if not `null`, forwards the output to the running processes standard out - see [GradleRunner.forwardOutput]
 * @param installation - if not `null`, use the specified installation - see [GradleRunner.withGradleInstallation]
 * @param versionNumber - if not `null`, use the specified version from Gradle's servers - see [GradleRunner.withGradleVersion]
 * @param forwardStdError- if not `null`, forwards build process `stdErr` to the supplied `Writer` - see [GradleRunner.forwardStdError]
 * @param forwardStdOutput- if not `null`, forwards build process `stdOut` to the supplied `Writer` - see [GradleRunner.forwardStdOutput]
 * @param usePluginClasspath- if `true`, uses the plugin classpath provided by the `java-gradle-plugin`. See [GradleRunner.withPluginClasspath].
 * @param testKitDir if not `null`, use the specified directory for TestKit storage needs - see [GradleRunner.withTestKitDir]
 * @param pluginClasspath if not `null`, sets the classpath files to use - see [GradleRunner.withPluginClasspath]
 * @param runnerConfigurer the additional configuration to apply to the `GradleRunner` before execution
 * @throws IllegalArgumentException when [forwardOutput] is `true` and [forwardStdOutput] is specified
 * @throws IllegalArgumentException when [forwardOutput] is `true` and [forwardStdError] is specified
 * @throws IllegalArgumentException when [usePluginClasspath] is `true` and [pluginClasspath] is specified
 */
@Deprecated(message = "Scheduled for removal", replaceWith = ReplaceWith("GradleRunner.setup"))
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
  usePluginClasspath: Boolean? = null,
  testKitDir: File? = null,
  pluginClasspath: Iterable<File>? = null,
  runnerConfigurer: RunnerConfigurer = ServiceLoaderRunnerConfigurer()
): BuildResult {
  require(!(forwardOutput == true && forwardStdOutput != null)) { "Cannot specify both forwardOutput and forwardStdOutput" }
  require(!(forwardOutput == true && forwardStdError != null)) { "Cannot specify both forwardOutput and forwardStdError" }
  require(!(usePluginClasspath == true && pluginClasspath != null)) { "Cannot specify both usePluginClasspath and forwardStdError" }
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
  pluginClasspath?.let { withPluginClasspath(pluginClasspath) }
  if (usePluginClasspath == true) {
    withPluginClasspath()
  }
  projectDir?.let { withProjectDir(it) }
  testKitDir?.let { withTestKitDir(it) }

  runnerConfigurer(this)
  return buildAndFail()
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
