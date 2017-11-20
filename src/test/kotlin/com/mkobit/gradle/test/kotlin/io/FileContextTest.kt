package com.mkobit.gradle.test.kotlin.io

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import testsupport.TempDirectory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.Random
import java.util.stream.Stream

@ExtendWith(TempDirectory::class)
internal class FileContextTest {
  @TestFactory
  internal fun `file attributes`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    fun newFile(fileName: String, content: ByteArray? = null) = Files.createFile(root.resolve(fileName)).also {
      if (content != null) {
        Files.write(it, content)
      }
    }
    fun newDirectory(directoryName: String) = Files.createDirectory(root.resolve(directoryName))
    val instant = Instant.from(
        LocalDateTime.of(2011, Month.NOVEMBER, 26, 7, 2)
            .atZone(ZoneId.systemDefault())
    )
    val clock = Clock.fixed(instant, ZoneId.systemDefault())
    return Stream.of(
        DynamicTest.dynamicTest("regular file modification time") {
          val context = FileContext.RegularFileContext(newFile("fileModTime"))
          Assertions.assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          Assertions.assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        DynamicTest.dynamicTest("directory modification time") {
          val context = FileContext.DirectoryContext(newDirectory("dirModTime"))
          Assertions.assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          Assertions.assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        // TODO: make a cross-platform test here to make sure true and false can both be tested
        DynamicTest.dynamicTest("regular file hidden status") {
          val context = FileContext.RegularFileContext(newFile("notHiddenFile"))
          Assertions.assertThat(context.isHidden)
              .isFalse()
        },
        DynamicTest.dynamicTest("directory hidden status") {
          val context = FileContext.DirectoryContext(newDirectory("notHiddenDir"))
          Assertions.assertThat(context.isHidden)
              .isFalse()
        },
        DynamicTest.dynamicTest("regular file size") {
          val bytes = 10
          val context = FileContext.RegularFileContext(newFile("fileSize", ByteArray(bytes, Int::toByte)))
          Assertions.assertThat(context.size)
              .isEqualTo(bytes.toLong())
              .isEqualTo(context.content.size.toLong())
        }
    )
  }

  @TestFactory
  internal fun `manage content with a RegularFileContext`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val randomFile = randomFileGenerator(root)
    // Create a RegularFileContext with a random file and its contents
    fun tempFileContext(initialContents: String? = null): FileContext.RegularFileContext =
        FileContext.RegularFileContext(randomFile(initialContents))

    return Stream.of(
        DynamicTest.dynamicTest("empty content can be read") {
          Assertions.assertThat(tempFileContext().content)
              .isEmpty()
        },
        DynamicTest.dynamicTest("content can be read") {
          val content = "here is some file content"
          tempFileContext(content)
        },
        DynamicTest.dynamicTest("empty content can be written") {
          val context = tempFileContext()
          context.content = ByteArray(0)
          Assertions.assertThat(context.content)
              .isEmpty()
          Assertions.assertThat(context.path)
              .hasContent("")
        },
        DynamicTest.dynamicTest("full content can be written") {
          val content = "this is some file content"
          val context = tempFileContext()
          context.content = content.toByteArray()
          Assertions.assertThat(context.content)
              .isEqualTo(content.toByteArray())
          Assertions.assertThat(context.path)
              .hasContent(content)
        },
        DynamicTest.dynamicTest("append content to existing file") {
          val originalContent = "this is the the original content"
          val appendedContent = "this is the appended content"
          val context = tempFileContext(originalContent)
          context.append(appendedContent.toByteArray())
          Assertions.assertThat(context.content)
              .startsWith(*originalContent.toByteArray())
              .endsWith(*appendedContent.toByteArray())
          Assertions.assertThat(context.path)
              .hasContent(originalContent + appendedContent)
        }
    )
  }

  @TestFactory
  internal fun `FileContext constructor validation`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val directory = Files.createDirectory(root.resolve("existingDirectory"))
    val doesntExist = root.resolve("dontexist")
    val regularFile = Files.createFile(root.resolve("existingFile"))
    val symlink = Files.createSymbolicLink(root.resolve("symlinkDestination"), Files.createFile(root.resolve("symlinkSource")))
    val directoryContextTests = Stream.of(
        DynamicTest.dynamicTest("DirectoryContext with nonexistent file throws exception") {
          Assertions.assertThatIllegalArgumentException()
              .isThrownBy { FileContext.DirectoryContext(doesntExist) }
        },
        DynamicTest.dynamicTest("DirectoryContext for regular file throws exception") {
          Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
              .isThrownBy { FileContext.DirectoryContext(regularFile) }
        },
        DynamicTest.dynamicTest("DirectoryContext for symlink throws exception") {
          Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
              .isThrownBy { FileContext.DirectoryContext(symlink) }
        }
    )

    val regularFileContextTests = Stream.of(
        DynamicTest.dynamicTest("RegularFileContext with nonexistent file throws exception") {
          Assertions.assertThatIllegalArgumentException().isThrownBy { FileContext.RegularFileContext(doesntExist) }
        },
        DynamicTest.dynamicTest("RegularFileContext for directory throws exception") {
          Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            FileContext.RegularFileContext(directory)
          }
        }
    )

    return Stream.concat(directoryContextTests, regularFileContextTests)
  }

  @TestFactory
  internal fun `FileRequest types`(): Stream<DynamicNode> {
    val posixFilePermissions = PosixFilePermissions.asFileAttribute(
        setOf(
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.GROUP_READ
        )
    )

    return Stream.of(
        DynamicTest.dynamicTest("Get type is an object") {
          Assertions.assertThat(FileAction.Get::class.objectInstance)
              .withFailMessage("${FileAction.Get::class} must be an object instance")
              .isNotNull()
        },
        DynamicTest.dynamicTest("MaybeCreate is a constant") {
          Assertions.assertThat(FileAction.MaybeCreate)
              .isInstanceOf(FileAction.MaybeCreate::class.java)
        },
        DynamicTest.dynamicTest("MaybeCreate instance with attributes") {
          val request = FileAction.MaybeCreate(listOf(posixFilePermissions))
          Assertions.assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        },
        DynamicTest.dynamicTest("Create is a constant-") {
          Assertions.assertThat(FileAction.Create)
              .isInstanceOf(FileAction.Create::class.java)
        },
        DynamicTest.dynamicTest("Create instance with attributes") {
          val request = FileAction.Create(listOf(posixFilePermissions))
          Assertions.assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.Get request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Get request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.Get

    return Stream.of(
        dynamicDirectoryContextTest("when file does not exist then an exception is thrown") {
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("doesntExist", requestType) }
        },
        dynamicDirectoryContextTest("when file exists directly in the directory then it can be retrieved") {
          val filePath = Files.write(it.path.resolve("fileExistPath"), listOf("content goes here"))
          val fileContext = it.file("fileExistPath", requestType)
          Assertions.assertThat(fileContext.path)
              .hasParent(it.path)
              .isRegularFile()
              .isEqualTo(filePath)
        },
        dynamicDirectoryContextTest("when file exists in a nested directory then it can be retrieved") {
          val dirPath = Files.createDirectories(it.path.resolve("some/nested/path/to/dir"))
          val filePath = Files.createFile(dirPath.resolve("existingFile"))
          val fileContext = it.file("some/nested/path/to/dir/existingFile", requestType)
          Assertions.assertThat(fileContext.path)
              .isRegularFile()
              .isEqualTo(filePath)
              .hasParent(dirPath)
        },
        dynamicDirectoryContextTest("when path is a directory then an exception is thrown") {
          Files.createDirectory(it.path.resolve("directory"))
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("directory", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.MaybeCreate request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_MaybeCreate request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.MaybeCreate

    return Stream.of(
        dynamicDirectoryContextTest("when file does not exist then it is created") {
          val fileContext = it.file("newFilePath", requestType)
          Assertions.assertThat(fileContext.path)
              .hasParent(it.path)
              .isRegularFile()
        },
        dynamicDirectoryContextTest("when file exists then it is retrieved") {
          val filePath = Files.createFile(it.path.resolve("fileExistPath"))
          val fileContext = it.file("fileExistPath", requestType)
          Assertions.assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(it.path)
              .isEqualTo(filePath)
        },
        dynamicDirectoryContextTest("when path is to a nonexistant file in an existing nested directory then it is retrieved") {
          val dirPath = Files.createDirectories(it.path.resolve("some/nested/dir"))
          val fileContext = it.file("some/nested/dir/nonExistentfilePath", requestType)
          Assertions.assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(dirPath)
        },
        dynamicDirectoryContextTest("when path is a directory then an exception is thrown") {
          Files.createDirectory(it.path.resolve("directory"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("directory", requestType) }
        },
        dynamicDirectoryContextTest("when path is a nested directory and the parent directory doesn't exist then an exception is thrown") {
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("directory/fileName", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.Create request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Create request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.Create

    return Stream.of(
        dynamicDirectoryContextTest("when the file does not exist then then it is created") {
          val fileContext = it.file("nonExistantFile", requestType)
          Assertions.assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(it.path)
        },
        dynamicDirectoryContextTest("when the path is nested and the parent directory doesn't exist then an exception is thrown") {
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("path/to/nonExistentDir/file", requestType) }
        },
        dynamicDirectoryContextTest("when the path is nested and the parent directory exists then the file is created") {
          val dirPath = Files.createDirectories(it.path.resolve("path/to/dir"))
          val context = it.file("path/to/dir/existingFile", requestType)
          Assertions.assertThat(context.path)
              .hasParent(dirPath)
              .isRegularFile()
        },
        dynamicDirectoryContextTest("when the file exists then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("existingFile", requestType) }
        },
        dynamicDirectoryContextTest("when the file exists in a nested directory then an exception is thrown") {
          val dirPath = Files.createDirectories(it.path.resolve("path/to/dir"))
          Files.createFile(dirPath.resolve("existingFile"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("path/to/dir/existingFile", requestType) }
        },
        dynamicDirectoryContextTest("when a directory exists at the path then an exception is thrown") {
          Files.createDirectory(it.path.resolve("dir"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("dir", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.Get request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Get request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.Get
    return Stream.of(
        dynamicDirectoryContextTest("when directory does not exist then an exception is thrown") {
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.directory("nonExistentDirectory", requestType) }
        },
        dynamicDirectoryContextTest("when direct child directory exists then it can be retrieved") {
          val directory = Files.createDirectory(it.path.resolve("dirPath"))
          val context = it.directory("dirPath", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .isEqualTo(directory)
        },
        dynamicDirectoryContextTest("when nested directory exists then it can be retrieved") {
          val directory = Files.createDirectories(it.path.resolve("path/to/nested/dir"))
          val context = it.directory("path/to/nested/dir", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .isEqualTo(directory)
        },
        dynamicDirectoryContextTest("when file exists at the path then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.directory("existingFile", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.MaybeCreate request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_MaybeCreate request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.MaybeCreate
    return Stream.of(
        dynamicDirectoryContextTest("when the directory does not exist then it is created") {
          val context = it.directory("nonExistentDirectory", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .hasParent(it.path)
              .hasFileName("nonExistentDirectory")
        },
        dynamicDirectoryContextTest("when the nested directory does not exists then the entire path is created") {
          val context = it.directory("path/to/nonexistent/dir", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
        },
        dynamicDirectoryContextTest("when some of the nested directory does not the exist then the entire path is created") {
          Files.createDirectories(it.path.resolve("path/to"))
          val context = it.directory("path/to/nonexistent/dir", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
        },
        dynamicDirectoryContextTest("when file exists at the path en an exception is thrown") {
          Files.createFile(it.path.resolve("regularFile"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("regularFile", requestType) }
        },
        dynamicDirectoryContextTest("when nested file exists at the path en an exception is thrown") {
          val dirPath = Files.createDirectories(it.path.resolve("nested/dir/path"))
          Files.createFile(dirPath.resolve("regularFile"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("nested/dir/path/regularFile", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileAction.Create request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Create request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileAction.Create

    return Stream.of(
        dynamicDirectoryContextTest("when the directory does not exist then then it is created") {
          val context = it.directory("dirName", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .hasParent(it.path)
              .hasFileName("dirName")
        },
        dynamicDirectoryContextTest("when the path is nested and the nested directory doesn't exist then the nested path is created") {
          val context = it.directory("nested/dir/path", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("nested/dir/path"))
        },
        dynamicDirectoryContextTest("when the path is nested and some of the parent directories exist then the full nested directory is created") {
          Files.createDirectories(it.path.resolve("nested/dir"))
          val context = it.directory("nested/dir/path", requestType)
          Assertions.assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("nested/dir/path"))
        },
        dynamicDirectoryContextTest("when the directory exists then an exception is thrown") {
          Files.createDirectory(it.path.resolve("existingDir"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("existingDir", requestType) }
        },
        dynamicDirectoryContextTest("when the directory exists in a nested directory then an exception is thrown") {
          Files.createDirectories(it.path.resolve("nested/path/to/existingDir"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("nested/path/to/existingDir", requestType) }
        },
        dynamicDirectoryContextTest("when a file exists at the path then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("existingFile", requestType) }
        }
    )
  }
}

/**
 * Helper function to create a factory for producing [FileContext.DirectoryContext].
 */
private fun newDirectoryContextFactory(root: Path): () -> FileContext.DirectoryContext {
  val randomName = randomStringGenerator()
  return { FileContext.DirectoryContext(Files.createDirectory(root.resolve(randomName()))) }
}

/**
 * Helper function to create a factory for producing [DynamicTest] that use a [FileContext.DirectoryContext].
 */
private fun dynamicDirectoryContextTestProvider(
    contextFactory: () -> FileContext.DirectoryContext
): (displayName: String, test: (FileContext.DirectoryContext) -> Unit) -> DynamicTest {
  return { displayName: String, test: (FileContext.DirectoryContext) -> Unit ->
    DynamicTest.dynamicTest(displayName) {
      test(contextFactory())
    }
  }
}

private fun randomStringGenerator(): () -> String {
  val random = Random()
  return { random.nextLong().toString() }
}

/**
 * Helper for generating random files with or without content in the provided directory.
 */
private fun randomFileGenerator(path: Path): (content: String?) -> Path {
  require(Files.isDirectory(path))
  val randomName = randomStringGenerator()

  return { content ->
    if (content != null) {
      Files.write(path.resolve(randomName()), content.toByteArray())
    } else {
      Files.createFile(path.resolve(randomName()))
    }
  }
}
