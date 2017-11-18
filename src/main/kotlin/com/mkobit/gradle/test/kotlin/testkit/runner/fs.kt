package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.time.Instant

/**
 * No operation.
 */
private val NoOp: Any.() -> Unit = {}

/**
 * Wraps the call and translates the exception.
 * @throws NoSuchFileException when [java.nio.file.NoSuchFileException] is thrown
 * @throws FileAlreadyExistsException when [java.nio.file.FileAlreadyExistsException] is thrown
 */
@Throws(NoSuchFileException::class, FileAlreadyExistsException::class)
private fun <T> translateIoExceptions(supplier: () -> T): T = try {
  supplier()
} catch (noSuchFile: java.nio.file.NoSuchFileException) {
  throw NoSuchFileException(File(noSuchFile.file)).initCause(noSuchFile)
} catch (alreadyExists: java.nio.file.FileAlreadyExistsException) {
  throw FileAlreadyExistsException(File(alreadyExists.file)).initCause(alreadyExists)
}

sealed class FileContext(val path: Path) {
  /**
   * The contents of the file.
   */
  var content: ByteArray
    get() = Files.readAllBytes(path)
    set(value) { Files.write(path, value) }

  /**
   * The file's last modified time.
   * @see Files.getLastModifiedTime
   * @see Files.setLastModifiedTime
   */
  var lastModifiedTime: Instant
    get() = Files.getLastModifiedTime(path).toInstant()
    set(value) { Files.setLastModifiedTime(path, FileTime.from(value)) }

  /**
   * Whether this file is considered hidden.
   * @see Files.isHidden
   */
  val isHidden: Boolean
    get() = Files.isHidden(path)

  /**
   * Size of file in bytes.
   * @see Files.size
   */
  val size: Long
    get() = Files.size(path)

  /**
   * Represents a regular file.
   * @property path the path of the regular file
   */
  class RegularFileContext(path: Path) : FileContext(path) {
    init {
      require(Files.isRegularFile(path)) { "Path $path must be a regular file" }
    }

    /**
     * Directly appends the provided [content] to the file.
     * @param content the content to append.
     */
    fun append(content: ByteArray) {
      Files.write(path, content, StandardOpenOption.APPEND)
    }
  }

  /**
   * Represents a directory.
   * @property path the path of the directory
   */
  class DirectoryContext(path: Path) : FileContext(path) {
    init {
      require(Files.isDirectory(path)) { "Path $path is not a directory" }
    }

    /**
     * Produce a [RegularFileContext] instance with a [Path] resolved from this instance's [path].
     *
     * @param fileName the filename to resolve in this directory
     * @param fileAction the action to take for the file
     * @param action the lambda that can provide additional setup of the file
     * @return a [RegularFileContext] for the resolved file
     */
    fun file(
        fileName: CharSequence,
        fileAction: FileAction = FileAction.MaybeCreate,
        action: RegularFileContext.() -> Unit = NoOp
    ): RegularFileContext {
      val filePath = path.resolve(fileName.toString())

      return translateIoExceptions {
        when (fileAction) {
          is FileAction.Get -> {
            if (!Files.isRegularFile(filePath)) {
              throw NoSuchFileException(filePath.toFile(), reason = "Regular file does not exist at $filePath")
            }
            RegularFileContext(filePath)
          }
          is FileAction.MaybeCreate -> {
            if (Files.exists(filePath)) {
              if (!Files.isRegularFile(filePath)) {
                throw FileAlreadyExistsException(filePath.toFile(), reason = "File at path $filePath already exists and is not a regular file")
              }
              RegularFileContext(filePath)
            } else {
              RegularFileContext(Files.createFile(filePath, *fileAction.fileAttributes.toTypedArray()))
            }
          }
          is FileAction.Create -> RegularFileContext(Files.createFile(filePath,
              *fileAction.fileAttributes.toTypedArray()))
        }.apply(action)
      }
    }

    @Throws(NoSuchFileException::class, FileAlreadyExistsException::class)
    fun directory(
        directoryName: CharSequence,
        fileAction: FileAction = FileAction.MaybeCreate,
        action: DirectoryContext.() -> Unit = NoOp
    ): DirectoryContext {
      val filePath = path.resolve(directoryName.toString())
      return translateIoExceptions {
        when (fileAction) {
          is FileAction.Get -> {
            if (!Files.isDirectory(filePath)) {
              throw NoSuchFileException(filePath.toFile(), reason = "Directory does not exist at $filePath")
            }
            DirectoryContext(filePath)
          }
          is FileAction.MaybeCreate -> {
            if (Files.exists(filePath)) {
              if (!Files.isDirectory(filePath)) {
                throw FileAlreadyExistsException(filePath.toFile(), reason = "File at path $filePath already exists and is not a directory")
              }
              DirectoryContext(filePath)
            } else {
              DirectoryContext(Files.createDirectories(filePath, *fileAction.fileAttributes.toTypedArray()))
            }
          }
          is FileAction.Create -> {
            if (Files.exists(filePath)) {
              throw FileAlreadyExistsException(filePath.toFile(), reason = "File at path $filePath already exists")
            }
            DirectoryContext(Files.createDirectories(filePath, *fileAction.fileAttributes.toTypedArray()))
          }
        }.apply(action)
      }
    }
  }
}

/**
 * A representation of how a file request should be handled.
 */
sealed class FileAction {
  /**
   * Get the request file object.
   */
  object Get : FileAction()

  companion object {
    /**
     * Get the file object if it already exists, otherwise create it with no specified options.
     */
    val MaybeCreate = MaybeCreate(emptyList())

    /**
     * Create the file with no specified options.
     */
    val Create = Create(emptyList())
  }

  /**
   * Get the object if it already exists, otherwise create it with the provided options.
   */
  data class MaybeCreate(val fileAttributes: List<FileAttribute<*>>) : FileAction()

  /**
   * Create the file with the provided properties.
   * @property fileAttributes the file attributes to create the file with
   */
  data class Create(val fileAttributes: List<FileAttribute<*>>) : FileAction()
}

/**
 * Setup this [GradleRunner.projectDirPath] with the provided [action].
 * @param action the action to apply to the [FileContext.DirectoryContext] instance
 */
fun GradleRunner.setupProjectDir(action: FileContext.DirectoryContext.() -> Unit): GradleRunner = apply {
  val path = projectDirPath ?: throw IllegalStateException("project directory must be specified")
  FileContext.DirectoryContext(path).run(action)
}

/**
 * The [GradleRunner.getProjectDir] as a [Path].
 */
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) {
    withProjectDir(value?.toFile())
  }
