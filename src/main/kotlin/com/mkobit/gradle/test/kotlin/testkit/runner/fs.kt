package com.mkobit.gradle.test.kotlin.testkit.runner

import com.mkobit.gradle.test.kotlin.io.FileContext
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path

/**
 * Setup this [GradleRunner.projectDirPath] with the provided [action].
 * @param action the action to apply to the [FileContext.DirectoryContext] instance
 */
fun GradleRunner.setupProjectDir(action: FileContext.DirectoryContext.() -> Unit): GradleRunner = apply {
  val path = projectDir?.toPath() ?: throw IllegalStateException("project directory must be specified")
  FileContext.DirectoryContext(path).run(action)
}

/**
 * The [GradleRunner.getProjectDir] as a [Path].
 */
@Deprecated("Superfluous getter method, will be removed", replaceWith = ReplaceWith("projectDir?.toPath()"), level = DeprecationLevel.ERROR)
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) {
    withProjectDir(value?.toFile())
  }
