package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import testsupport.TempDirectory
import testsupport.dynamicGradleRunnerTest
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.Random
import java.util.stream.Stream

@ExtendWith(TempDirectory::class)
internal class GradleRunnerFsExtensionsTest {

  @TestFactory
  internal fun `projectDirPath extension`(): Stream<DynamicNode> {
    return Stream.of(
        dynamicGradleRunnerTest("projectDir is null") {
          assertThat(projectDirPath)
              .isNull()
        },
        dynamicGradleRunnerTest("projectDir.toPath() is equal to the projectDirPath") {
          val file = File("/tmp")
          withProjectDir(file)
          assertThat(projectDirPath)
              .isEqualTo(file.toPath())
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
        dynamicTest("empty content can be read") {
          assertThat(tempFileContext().content)
              .isEmpty()
        },
        dynamicTest("content can be read") {
          val content = "here is some file content"
          tempFileContext(content)
        },
        dynamicTest("empty content can be written") {
          val context = tempFileContext()
          context.content = ByteArray(0)
          assertThat(context.content)
              .isEmpty()
          assertThat(context.path)
              .hasContent("")
        },
        dynamicTest("full content can be written") {
          val content = "this is some file content"
          val context = tempFileContext()
          context.content = content.toByteArray()
          assertThat(context.content)
              .isEqualTo(content.toByteArray())
          assertThat(context.path)
              .hasContent(content)
        },
        dynamicTest("append content to existing file") {
          val originalContent = "this is the the original content"
          val appendedContent = "this is the appended content"
          val context = tempFileContext(originalContent)
          context.append(appendedContent.toByteArray())
          assertThat(context.content)
              .startsWith(*originalContent.toByteArray())
              .endsWith(*appendedContent.toByteArray())
          assertThat(context.path)
              .hasContent(originalContent + appendedContent)
        }
    )
  }

  @Test
  internal fun `files created in projectDir`() {
//    val runner = GradleRunner.create()
//    runner.file("build.gradle") {
//      "this is the file content"
//    }
//    runner.mkdir
  }

  @TestFactory
  internal fun `FileContext constructor validation`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val directory = Files.createDirectory(root.resolve("existingDirectory"))
    val doesntExist = root.resolve("dontexist")
    val regularFile = Files.createFile(root.resolve("existingFile"))
    val symlink = Files.createSymbolicLink(root.resolve("symlinkDestination"), Files.createFile(root.resolve("symlinkSource")))
    val directoryContextTests = Stream.of(
        dynamicTest("DirectoryContext with nonexistent file throws exception") {
          assertThatIllegalArgumentException()
              .isThrownBy { FileContext.DirectoryContext(doesntExist) }
        },
        dynamicTest("DirectoryContext for regular file throws exception") {
          assertThatExceptionOfType(IllegalArgumentException::class.java)
              .isThrownBy { FileContext.DirectoryContext(regularFile) }
        },
        dynamicTest("DirectoryContext for symlink throws exception") {
          assertThatExceptionOfType(IllegalArgumentException::class.java)
              .isThrownBy { FileContext.DirectoryContext(symlink) }
        }
    )

    val regularFileContextTests = Stream.of(
        dynamicTest("RegularFileContext with nonexistent file throws exception") {
          assertThatIllegalArgumentException().isThrownBy { FileContext.RegularFileContext(doesntExist) }
        },
        dynamicTest("RegularFileContext for directory throws exception") {
          assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { FileContext.RegularFileContext(directory) }
        }
    )

    return Stream.concat(directoryContextTests, regularFileContextTests)
  }

  @Test
  internal fun `cannot configure GradleRunner project directory if it is not set`() {
    assertThatThrownBy {
      GradleRunner.create().setupProjectDir {  }
    }.isInstanceOf(IllegalStateException::class.java)
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
        dynamicTest("Get type is an object") {
          assertThat(FileRequest.Get::class.objectInstance)
              .withFailMessage("${FileRequest.Get::class} must be an object instance")
              .isNotNull()
        },
        dynamicTest("MaybeCreate is a constant") {
          assertThat(FileRequest.MaybeCreate)
              .isInstanceOf(FileRequest.MaybeCreate::class.java)
        },
        dynamicTest("MaybeCreate instance with attributes") {
          val request = FileRequest.MaybeCreate(listOf(posixFilePermissions))
          assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        },
        dynamicTest("Create is a constant-") {
          assertThat(FileRequest.Create)
              .isInstanceOf(FileRequest.Create::class.java)
        },
        dynamicTest("Create instance with attributes") {
          val request = FileRequest.Create(listOf(posixFilePermissions))
          assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.Get request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Get request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.Get

    return Stream.of(
        dynamicDirectoryContextTest("when file does not exist then an exception is thrown") {
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("doesntExist", requestType) }
        },
        dynamicDirectoryContextTest("when file exists directly in the directory then it can be retrieved") {
          val filePath = Files.write(it.path.resolve("fileExistPath"), listOf("content goes here"))
          val fileContext = it.file("fileExistPath", requestType)
          assertThat(fileContext.path)
              .hasParent(it.path)
              .isRegularFile()
              .isEqualTo(filePath)
        },
        dynamicDirectoryContextTest("when file exists in a nested directory then it can be retrieved") {
          val dirPath = Files.createDirectories(it.path.resolve("some/nested/path/to/dir"))
          val filePath = Files.createFile(dirPath.resolve("existingFile"))
          val fileContext = it.file("some/nested/path/to/dir/existingFile", requestType)
          assertThat(fileContext.path)
              .isRegularFile()
              .isEqualTo(filePath)
              .hasParent(dirPath)
        },
        dynamicDirectoryContextTest("when path is a directory then an exception is thrown") {
          Files.createDirectory(it.path.resolve("directory"))
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("directory", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.MaybeCreate request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_MaybeCreate request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.MaybeCreate

    return Stream.of(
        dynamicDirectoryContextTest("when file does not exist then it is created") {
          val fileContext = it.file("newFilePath", requestType)
          assertThat(fileContext.path)
              .hasParent(it.path)
              .isRegularFile()
        },
        dynamicDirectoryContextTest("when file exists then it is retrieved") {
          val filePath = Files.createFile(it.path.resolve("fileExistPath"))
          val fileContext = it.file("fileExistPath", requestType)
          assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(it.path)
              .isEqualTo(filePath)
        },
        dynamicDirectoryContextTest("when path is to a nonexistant file in an existing nested directory then it is retrieved") {
          val dirPath = Files.createDirectories(it.path.resolve("some/nested/dir"))
          val fileContext = it.file("some/nested/dir/nonExistentfilePath", requestType)
          assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(dirPath)
        },
        dynamicDirectoryContextTest("when path is a directory then an exception is thrown") {
          Files.createDirectory(it.path.resolve("directory"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("directory", requestType) }
        },
        dynamicDirectoryContextTest("when path is a nested directory and the parent directory doesn't exist then an exception is thrown") {
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("directory/fileName", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.Create request type for a file")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Create request type for a file`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.Create

    return Stream.of(
        dynamicDirectoryContextTest("when the file does not exist then then it is created") {
          val fileContext = it.file("nonExistantFile", requestType)
          assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(it.path)
        },
        dynamicDirectoryContextTest("when the path is nested and the parent directory doesn't exist then an exception is thrown") {
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.file("path/to/nonExistentDir/file", requestType) }
        },
        dynamicDirectoryContextTest("when the path is nested and the parent directory exists then the file is created") {
          val dirPath = Files.createDirectories(it.path.resolve("path/to/dir"))
          val context = it.file("path/to/dir/existingFile", requestType)
          assertThat(context.path)
              .hasParent(dirPath)
              .isRegularFile()
        },
        dynamicDirectoryContextTest("when the file exists then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("existingFile", requestType) }
        },
        dynamicDirectoryContextTest("when the file exists in a nested directory then an exception is thrown") {
          val dirPath = Files.createDirectories(it.path.resolve("path/to/dir"))
          Files.createFile(dirPath.resolve("existingFile"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("path/to/dir/existingFile", requestType) }
        },
        dynamicDirectoryContextTest("when a directory exists at the path then an exception is thrown") {
          Files.createDirectory(it.path.resolve("dir"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.file("dir", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.Get request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Get request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.Get
    return Stream.of(
        dynamicDirectoryContextTest("when directory does not exist then an exception is thrown") {
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.directory("nonExistentDirectory", requestType) }
        },
        dynamicDirectoryContextTest("when direct child directory exists then it can be retrieved") {
          val directory = Files.createDirectory(it.path.resolve("dirPath"))
          val context = it.directory("dirPath", requestType)
          assertThat(context.path)
              .isDirectory()
              .isEqualTo(directory)
        },
        dynamicDirectoryContextTest("when nested directory exists then it can be retrieved") {
          val directory = Files.createDirectories(it.path.resolve("path/to/nested/dir"))
          val context = it.directory("path/to/nested/dir", requestType)
          assertThat(context.path)
              .isDirectory()
              .isEqualTo(directory)
        },
        dynamicDirectoryContextTest("when file exists at the path then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          assertThatExceptionOfType(NoSuchFileException::class.java)
              .isThrownBy { it.directory("existingFile", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.MaybeCreate request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_MaybeCreate request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.MaybeCreate
    return Stream.of(
        dynamicDirectoryContextTest("when the directory does not exist then it is created") {
          val context = it.directory("nonExistentDirectory", requestType)
          assertThat(context.path)
              .isDirectory()
              .hasParent(it.path)
              .hasFileName("nonExistentDirectory")
        },
        dynamicDirectoryContextTest("when the nested directory does not exists then the entire path is created") {
          val context = it.directory("path/to/nonexistent/dir", requestType)
          assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
        },
        dynamicDirectoryContextTest("when some of the nested directory does not the exist then the entire path is created") {
          Files.createDirectories(it.path.resolve("path/to"))
          val context = it.directory("path/to/nonexistent/dir", requestType)
          assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
        },
        dynamicDirectoryContextTest("when file exists at the path en an exception is thrown") {
          Files.createFile(it.path.resolve("regularFile"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("regularFile", requestType) }
        },
        dynamicDirectoryContextTest("when nested file exists at the path en an exception is thrown") {
          val dirPath = Files.createDirectories(it.path.resolve("nested/dir/path"))
          Files.createFile(dirPath.resolve("regularFile"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("nested/dir/path/regularFile", requestType) }
        }
    )
  }

  @DisplayName("Given DirectoryContext and FileRequest.Create request type for a directory")
  @TestFactory
  internal fun `Given DirectoryContext and FileRequest_Create request type for a directory`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
    val dynamicDirectoryContextTest = dynamicDirectoryContextTestProvider(newDirectoryContextFactory(root))
    val requestType = FileRequest.Create

    return Stream.of(
        dynamicDirectoryContextTest("when the directory does not exist then then it is created") {
          val context = it.directory("dirName", requestType)
          assertThat(context.path)
              .isDirectory()
              .hasParent(it.path)
              .hasFileName("dirName")
        },
        dynamicDirectoryContextTest("when the path is nested and the nested directory doesn't exist then the nested path is created") {
          val context = it.directory("nested/dir/path", requestType)
          assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("nested/dir/path"))
        },
        dynamicDirectoryContextTest("when the path is nested and some of the parent directories exist then the full nested directory is created") {
          Files.createDirectories(it.path.resolve("nested/dir"))
          val context = it.directory("nested/dir/path", requestType)
          assertThat(context.path)
              .isDirectory()
              .startsWith(it.path)
              .endsWith(Paths.get("nested/dir/path"))
        },
        dynamicDirectoryContextTest("when the directory exists then an exception is thrown") {
          Files.createDirectory(it.path.resolve("existingDir"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("existingDir", requestType) }
        },
        dynamicDirectoryContextTest("when the directory exists in a nested directory then an exception is thrown") {
          Files.createDirectories(it.path.resolve("nested/path/to/existingDir"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("nested/path/to/existingDir", requestType) }
        },
        dynamicDirectoryContextTest("when a file exists at the path then an exception is thrown") {
          Files.createFile(it.path.resolve("existingFile"))
          assertThatExceptionOfType(FileAlreadyExistsException::class.java)
              .isThrownBy { it.directory("existingFile", requestType) }
        }
    )
  }

  @Test
  internal fun `set up a Gradle project using full DSL`(@TempDirectory.Root root: Path) {
    GradleRunner.create().withProjectDir(root.toFile()).setupProjectDir {
      file("settings.gradle") { content = "// settings.gradle".toByteArray()}
      file("build.gradle") { content = "// build.gradle".toByteArray() }
      directory("src/main/java") {
        file("MainClass.java") { content = "public class Hello {}".toByteArray() }
        directory("com/mkobit/") {
          file("NestedDude.java") {
            content = "package com.mkobit;${System.lineSeparator()}".toByteArray()
            append("public class NestedDude {}${System.lineSeparator()}".toByteArray())
            append("// Additional appended content${System.lineSeparator()}".toByteArray())
          }
        }
      }
    }

    SoftAssertions.assertSoftly {
      it.apply {
        assertThat(root.resolve("settings.gradle"))
            .isRegularFile
            .hasContent("// settings.gradle")
        assertThat(root.resolve("build.gradle"))
            .isRegularFile
            .hasContent("// build.gradle")
        assertThat(root.resolve("src/main/java"))
            .isDirectory
        assertThat(root.resolve("src/main/java/MainClass.java"))
            .isRegularFile
            .hasContent("public class Hello {}")
        assertThat(root.resolve("src/main/java/com/mkobit"))
            .isDirectory
        assertThat(root.resolve("src/main/java/com/mkobit/NestedDude.java"))
            .isRegularFile
            .hasContent("""
              package com.mkobit;
              public class NestedDude {}
              // Additional appended content
              """.trimIndent())
      }
    }
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
    dynamicTest(displayName) {
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
