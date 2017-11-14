package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute

/**
 * Require that the provided [path] is not an absolute path.
 * @throws IllegalArgumentException if path is absolute
 */
private fun requireNonAbsolutePath(path: Path) = require(!path.isAbsolute) { "Path $path must not be absolute" }

sealed class FileContext(val path: Path) {

  class RegularFileContext(path: Path) : FileContext(path) {
    init {
      require(Files.isRegularFile(path)) { "Path $path must be a regular file" }
    }

    fun siblingFile(fileName: CharSequence, charset: Charset = Charsets.UTF_8, content: () -> CharSequence): RegularFileContext =
        RegularFileContext(Files.write(path.resolveSibling(fileName.toString()), content().toString().toByteArray(charset)))

    fun append(charset: Charset = Charsets.UTF_8, content: () -> CharSequence): RegularFileContext =
        RegularFileContext(Files.write(path, content().toString().toByteArray(charset), StandardOpenOption.APPEND))

    fun write(charset: Charset = Charsets.UTF_8, content: () -> CharSequence): RegularFileContext =
        RegularFileContext(Files.write(path, content().toString().toByteArray(charset)))
  }
  class DirectoryContext(path: Path) : FileContext(path) {
    init {
      require(Files.isDirectory(path)) { "Path $path must be a directory" }
    }

    /**
     * Get a [RegularFileContext] instance with a [Path] resolved [path] with this context's [path] and the
     * provided [fileName]. The file must already exist.
     *
     * @param fileName the filename to resolve in this directory
     */
    fun file(fileName: CharSequence): RegularFileContext = RegularFileContext(path.resolve(fileName.toString()))

    fun file(fileName: CharSequence, charset: Charset = Charsets.UTF_8, content: () -> CharSequence): RegularFileContext =
        RegularFileContext(Files.write(path.resolve(fileName.toString()), content().toString().toByteArray(charset)))


    fun directory(directoryName: CharSequence, fileAttributes: Set<FileAttribute<Any>>,directoryContextAction: DirectoryContext.() -> Unit): DirectoryContext =
        path.resolve(directoryName.toString()).let { dirPath ->
          DirectoryContext(Files.createDirectories(dirPath, *fileAttributes.toTypedArray())).apply {
            directoryContextAction()
          }
        }
    }
  }
}


/**
 * The [GradleRunner.getProjectDir] as a [Path].
 */
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) { withProjectDir(value?.toFile()) }
