package com.mkobit.gradle.test.kotlin.io

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
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
        dynamicTest("regular file modification time") {
          val context = FileContext.RegularFileContext(newFile("fileModTime"))
          assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        dynamicTest("directory modification time") {
          val context = FileContext.DirectoryContext(newDirectory("dirModTime"))
          assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        // TODO: make a cross-platform test here to make sure true and false can both be tested
        dynamicTest("regular file hidden status") {
          val context = FileContext.RegularFileContext(newFile("notHiddenFile"))
          assertThat(context.isHidden)
              .isFalse()
        },
        dynamicTest("directory hidden status") {
          val context = FileContext.DirectoryContext(newDirectory("notHiddenDir"))
          assertThat(context.isHidden)
              .isFalse()
        },
        dynamicTest("regular file size") {
          val bytes = 10
          val context = FileContext.RegularFileContext(newFile("fileSize", ByteArray(bytes, Int::toByte)))
          assertThat(context.size)
              .isEqualTo(bytes.toLong())
              .isEqualTo(context.content.size.toLong())
        }
    )
  }

  @Nested
  inner class RegularFileContextTest {
    private lateinit var fileContext: FileContext.RegularFileContext

    @BeforeEach
    internal fun setUp(@TempDirectory.Root root: Path, testInfo: TestInfo) {
      fileContext = FileContext.RegularFileContext(Files.createFile(root.resolve(testInfo.displayName)))
    }

    @TestFactory
    internal fun `constructor validation`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
      val directory = Files.createDirectory(root.resolve("existingDirectory"))
      val doesntExist = root.resolve("dontexist")
      val regularFile = Files.createFile(root.resolve("existingFile"))

      return Stream.of(
          dynamicTest("constructed with nonexistent file throws an IllegalArgumentException") {
            assertThatIllegalArgumentException().isThrownBy { FileContext.RegularFileContext(doesntExist) }
          },
          dynamicTest("constructed with directory throws an IllegalArgumentException") {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
              FileContext.RegularFileContext(directory)
            }
          },
          dynamicTest("constructed with an existing file does not throw any exception") {
            assertThatCode {
              FileContext.RegularFileContext(regularFile)
            }.doesNotThrowAnyException()
          }
      )
    }

    @Test
    internal fun `empty content can be read`() {
      assertThat(fileContext.content)
          .isEmpty()
    }

    @Test
    internal fun `content can be read`() {
      val content = "here is some file content".toByteArray()
      Files.write(fileContext.path, content)
      assertThat(fileContext.content)
          .isEqualTo(content)
    }

    @Test
    internal fun `empty content can be written`() {
      fileContext.content = ByteArray(0)
      assertThat(fileContext.content)
          .isEmpty()
      assertThat(fileContext.path)
          .hasContent("")
    }

    @Test
    internal fun `entire contents can be written`() {
      val content = "this is some file content"
      fileContext.content = content.toByteArray()
      assertThat(fileContext.content)
          .isEqualTo(content.toByteArray())
      assertThat(fileContext.path)
          .hasContent(content)
    }

    @Test
    internal fun `append content to existing file`() {
      val originalContent = "this is the the original content"
      val appendedContent = "this is the appended content"
      fileContext.content = originalContent.toByteArray()
      fileContext.append(appendedContent.toByteArray())
      assertThat(fileContext.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*appendedContent.toByteArray())
      assertThat(fileContext.path)
          .hasContent(originalContent + appendedContent)
    }
  }

  @Nested
  inner class DirectoryContextTest {

    private lateinit var directoryContext: FileContext.DirectoryContext

    @BeforeEach
    internal fun setUp(@TempDirectory.Root root: Path, testInfo: TestInfo) {
      directoryContext = FileContext.DirectoryContext(Files.createDirectories(root.resolve(testInfo.displayName)))
    }

    @TestFactory
    internal fun `constructor validation`(@TempDirectory.Root root: Path): Stream<DynamicNode> {
      val directory = Files.createDirectory(root.resolve("existingDirectory"))
      val doesntExist = root.resolve("dontexist")
      val regularFile = Files.createFile(root.resolve("existingFile"))
      val symlink = Files.createSymbolicLink(root.resolve("symlinkDestination"),
          Files.createFile(root.resolve("symlinkSource")))
      return Stream.of(
          dynamicTest("constructed with nonexistent file throws IllegalArgumentException") {
            assertThatIllegalArgumentException()
                .isThrownBy { FileContext.DirectoryContext(doesntExist) }
          },
          dynamicTest("constructed with regular file throws IllegalArgumentException") {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { FileContext.DirectoryContext(regularFile) }
          },
          dynamicTest("constructed with symlink throws IllegalArgumentException") {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { FileContext.DirectoryContext(symlink) }
          },
          dynamicTest("constructed with existing directory does not throw an exception") {
            assertThatCode { FileContext.DirectoryContext(directory) }
                .doesNotThrowAnyException()
          }
      )
    }

    @Nested
    inner class FileActionMaybeCreate {
      private val requestType = FileAction.MaybeCreate

      @Test
      internal fun `when file does not exist and file is requested then it is created`() {
        val filename = "newFileName"
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .hasFileName(filename)
      }

      @Test
      internal fun `when file exists and file is requested then context is retrieved`() {
        val filename = "fileAlreadyExists"
        val filePath = Files.createFile(directoryContext.path.resolve(filename))
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
            .isEqualTo(filePath)
      }

      @Test
      internal fun `when path is to a nonexistent file in an existing nested directory and file is requested then context is retrieved`() {
        val filename = "nonExistentFile"
        val dirPath = Files.createDirectories(directoryContext.path.resolve("some/nested/dir"))
        val fileContext = directoryContext.file("some/nested/dir/$filename", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(dirPath)
            .hasFileName(filename)
      }

      @Test
      internal fun `when path is a directory and file is requested then a FileAlreadyExistsException is thrown`() {
        Files.createDirectory(directoryContext.path.resolve("directory"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.file("directory", requestType) }
      }

      @Test
      internal fun `when path is a nested directory, the parent directory doesn't exist, and fil eis requested then an exception is thrown`() {
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.file("directory/fileName", requestType) }
      }

      @Test
      internal fun `when the directory does not exist and directory is requested then it is created`() {
        val directoryName = "nonExistentDirectory"
        val context = directoryContext.directory(directoryName, requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName(directoryName)
      }

      @Test
      internal fun `when the nested directory does not exist and directory is requested then the entire path is created`() {
        val context = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test
      internal fun `when some of the nested directory does not exist then the entire path is created`() {
        Files.createDirectories(directoryContext.path.resolve("path/to"))
        val context = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test
      internal fun `when file exists at the path and directory is requested then FileAlreadyExistsException is thrown`() {
        Files.createFile(directoryContext.path.resolve("regularFile"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.directory("regularFile", requestType) }
      }

      @Test
      internal fun `when nested file exists at the path and directory is requested then FileAlreadyExistsException is thrown`() {
        val dirPath = Files.createDirectories(directoryContext.path.resolve("nested/dir/path"))
        Files.createFile(dirPath.resolve("regularFile"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.directory("nested/dir/path/regularFile", requestType) }
      }

      @TestFactory
      internal fun `when string invocation is used and directory does not exist then the directory is created`(): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val context = directoryContext.run {
                "filename1"(requestType) {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.DirectoryContext::class.java)
              assertThat(context.path)
                  .isDirectory()
                  .hasParent(directoryContext.path)
                  .hasFileName("filename1")
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val context = directoryContext.run {
                "filename2" {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.DirectoryContext::class.java)
              assertThat(context.path)
                  .isDirectory()
                  .hasParent(directoryContext.path)
                  .hasFileName("filename2")
            }
        )
      }

      @TestFactory
      internal fun `when string invocation is used and directory exists then it is retrieved`(): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val context = directoryContext.run {
                Files.createDirectory(directoryContext.path.resolve("filename1"))
                "filename1"(requestType) {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.DirectoryContext::class.java)
              assertThat(context.path)
                  .isDirectory()
                  .hasParent(directoryContext.path)
                  .hasFileName("filename1")
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val context = directoryContext.run {
                Files.createDirectory(directoryContext.path.resolve("filename2"))
                "filename2" {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.DirectoryContext::class.java)
              assertThat(context.path)
                  .isDirectory()
                  .hasParent(directoryContext.path)
                  .hasFileName("filename2")
            }
        )
      }

      @TestFactory
      internal fun `when string invocation is used and file exists at path then FileAlreadyExistsException is thrown`(): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              directoryContext.run {
                Files.createFile(directoryContext.path.resolve("filename1"))
                assertThatExceptionOfType(FileAlreadyExistsException::class.java)
                    .isThrownBy {
                      "filename1"(requestType) {}
                    }
              }
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              directoryContext.run {
                Files.createFile(directoryContext.path.resolve("filename2"))
                assertThatExceptionOfType(FileAlreadyExistsException::class.java)
                    .isThrownBy {
                      "filename2" {}
                    }
              }
            }
        )
      }

      @TestFactory
      internal fun `when string invocation is used with content and file does not exist then the file is created with the provided content`(): Stream<DynamicNode> {
        val content = "this is file content"
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val context = directoryContext.run {
                "filename1"(requestType, content = content)
              }
              assertThat(context)
                  .isInstanceOf(FileContext.RegularFileContext::class.java)
              assertThat(context.path)
                  .hasContent(content)
                  .hasParent(directoryContext.path)
                  .hasFileName("filename1")
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val context = directoryContext.run {
                 "filename2"(content = content)
              }
              assertThat(context)
                  .isInstanceOf(FileContext.RegularFileContext::class.java)
              assertThat(context.path)
                  .hasContent(content)
                  .hasParent(directoryContext.path)
                  .hasFileName("filename2")
            }
        )
      }

      @Disabled("Undefined behaviour for this right now. Should empty content be written? Should it be appended? Should option sfor both be provided? When decided, this test should also be renamed")
      @TestFactory
      internal fun `when string invocation is called, file already exists, and the provided content is Original, then the file is retrieved with its original content`(): Stream<DynamicNode> {
        val content = "this is the initial file content".toByteArray()
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val filename = "filename1"
              Files.write(Files.createFile(directoryContext.path.resolve(filename)), content)
              val context = directoryContext.run {
                filename(requestType, content = null)
              }
              Assertions.fail("undecided behaviour right now")
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val context = directoryContext.run {
               "filename2"(content = null)
              }
              Assertions.fail("undecided behaviour right now")
            }
        )
      }

      @TestFactory
      internal fun `when string invocation is used with content and directory exists then FileAlreadyExistsException is thrown`(): Stream<DynamicNode> {
        val content = "this is file content"
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              directoryContext.run {
                Files.createDirectory(directoryContext.path.resolve("filename1"))
                assertThatExceptionOfType(FileAlreadyExistsException::class.java)
                    .isThrownBy {
                      "filename1"(requestType, content = content)
                    }
              }
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              directoryContext.run {
                Files.createDirectory(directoryContext.path.resolve("filename2"))
                assertThatExceptionOfType(FileAlreadyExistsException::class.java)
                    .isThrownBy {
                      "filename2"(content = content)
                    }
              }
            }
        )
      }
    }

    @Nested
    inner class FileActionCreate {
      private val requestType = FileAction.Create

      @Test
      internal fun `when the file does not exist and file is requested then it is created`() {
        val fileContext = directoryContext.file("nonExistantFile", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
      }

      @Test
      internal fun `when the path is nested and the parent directory doesn't exist then an exception is thrown`() {
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.file("path/to/nonExistentDir/file", requestType) }
      }

      @Test
      internal fun `when the path is nested, the parent directory exists, and file is requested then the file is created`() {
        val dirPath = Files.createDirectories(directoryContext.path.resolve("path/to/dir"))
        val context = directoryContext.file("path/to/dir/existingFile", requestType)
        assertThat(context.path)
            .hasParent(dirPath)
            .isRegularFile()
      }

      @Test
      internal fun `when the file exists and file is requested then a FileAlreadyExistsException is thrown`() {
        val filename = "existingFile"
        Files.createFile(directoryContext.path.resolve(filename))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.file(filename, requestType) }
      }

      @Test
      internal fun `when the file already exists in a nested directory then a FileAlreadyExistsException is thrown`() {
        val dirPath = Files.createDirectories(directoryContext.path.resolve("path/to/dir"))
        val filename = "existingFile"
        Files.createFile(dirPath.resolve(filename))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.file("path/to/dir/$filename", requestType) }
      }

      @Test
      internal fun `when a directory already exists at the path and file is requested then a FileAlreadyExistsException is thrown`() {
        val directoryName = "directory"
        Files.createDirectory(directoryContext.path.resolve(directoryName))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.file(directoryName, requestType) }
      }

      @Test
      internal fun `when the directory does not exist then then it is created`() {
        val context = directoryContext.directory("dirName", requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName("dirName")
      }
      @Test
      internal fun `when the path is nested and the nested directory doesn't exist then the nested path is created`() {
        val context = directoryContext.directory("nested/dir/path", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("nested/dir/path"))
      }
      @Test
      internal fun `when the path is nested and some of the parent directories exist and directory is requested then the full nested directory is created`() {
        Files.createDirectories(directoryContext.path.resolve("nested/dir"))
        val context = directoryContext.directory("nested/dir/path", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("nested/dir/path"))
      }
      @Test
      internal fun `when the directory exists and directory is requested then an exception is thrown`() {
        Files.createDirectory(directoryContext.path.resolve("existingDir"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.directory("existingDir", requestType) }
      }
      @Test
      internal fun `when the directory exists and directory is requested in a nested directory then an exception is thrown`() {
        Files.createDirectories(directoryContext.path.resolve("nested/path/to/existingDir"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.directory("nested/path/to/existingDir", requestType) }
      }
      @Test
      internal fun `when a file exists at the path and directory is requested then an exception is thrown`() {
        Files.createFile(directoryContext.path.resolve("existingFile"))
        assertThatExceptionOfType(FileAlreadyExistsException::class.java)
            .isThrownBy { directoryContext.directory("existingFile", requestType) }
      }
    }

    @Nested
    inner class FileActionGet {
      private val requestType = FileAction.Get

      @Test
      internal fun `when file does not exist then an exception is thrown`() {
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.file("doesntExist", requestType) }
      }

      @Test
      internal fun `when file exists directly in the directory then directoryContext can be retrieved`() {
        val filePath = Files.createFile(directoryContext.path.resolve("fileExistPath"))
        val fileContext = directoryContext.file("fileExistPath", requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .isEqualTo(filePath)
      }

      @Test
      internal fun `when file exists in a nested directory then directoryContext can be retrieved`() {
        val dirPath = Files.createDirectories(directoryContext.path.resolve("some/nested/path/to/dir"))
        val filePath = Files.createFile(dirPath.resolve("existingFile"))
        val fileContext = directoryContext.file("some/nested/path/to/dir/existingFile", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .isEqualTo(filePath)
            .hasParent(dirPath)
      }

      @Test
      internal fun `when path is a directory then an exception is thrown`() {
        Files.createDirectory(directoryContext.path.resolve("directory"))
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.file("directory", requestType) }
      }

      @Test
      internal fun `"when directory does not exist and directory is requested then a NoSuchFileException is thrown`() {
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.directory("nonExistentDirectory", requestType) }
      }

      @Test
      internal fun `when direct child directory exists then directoryContext can be retrieved`() {
        val directory = Files.createDirectory(directoryContext.path.resolve("dirPath"))
        val context = directoryContext.directory("dirPath", requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test
      internal fun `when nested directory exists and directory is requested then it is retrieved`() {
        val directory = Files.createDirectories(directoryContext.path.resolve("path/to/nested/dir"))
        val context = directoryContext.directory("path/to/nested/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test
      internal fun `when file exists and directory is requested then a NoSuchFileException is thrown`() {
        val filename = "existingFile"
        Files.createFile(directoryContext.path.resolve(filename))
        assertThatExceptionOfType(NoSuchFileException::class.java)
            .isThrownBy { directoryContext.directory(filename, requestType) }
      }
    }
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
          assertThat(FileAction.Get::class.objectInstance)
              .withFailMessage("${FileAction.Get::class} must be an object instance")
              .isNotNull()
        },
        dynamicTest("MaybeCreate is a constant") {
          assertThat(FileAction.MaybeCreate)
              .isInstanceOf(FileAction.MaybeCreate::class.java)
        },
        dynamicTest("MaybeCreate instance with attributes") {
          val request = FileAction.MaybeCreate(listOf(posixFilePermissions))
          assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        },
        dynamicTest("Create is a constant") {
          assertThat(FileAction.Create)
              .isInstanceOf(FileAction.Create::class.java)
        },
        dynamicTest("Create instance with attributes") {
          val request = FileAction.Create(listOf(posixFilePermissions))
          assertThat(request.fileAttributes)
              .containsExactly(posixFilePermissions)
        }
    )
  }
}
