package com.mkobit.gradle.test.kotlin.io

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import testsupport.assertThatFileAlreadyExistsException
import testsupport.assertThatNoSuchFileException
import testsupport.fileNameString
import testsupport.newDirectory
import testsupport.newFile
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
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(TempDirectory::class)
internal class FileContextTest {

  @Nested inner class OriginalTest {
    @Test internal fun `Original is a singleton object`() {
      assertThat(Original::class.objectInstance)
          .describedAs("Is object instance")
          .isNotNull()
    }

    @DisplayName("CharSequence.length throws UnsupportedOperationException")
    @Test
    internal fun `CharSequence_length throws UnsupportedOperationException`() {
      assertThatExceptionOfType(UnsupportedOperationException::class.java)
        .isThrownBy {
          Original.length
        }.withMessageContaining("Cannot access length from com.mkobit.gradle.test.kotlin.io.Original")
    }

    @DisplayName("CharSequence.get(index) throws UnsupportedOperationException")
    @Test
    internal fun `CharSequence_get throws UnsupportedOperationException`() {
      assertThatExceptionOfType(UnsupportedOperationException::class.java)
        .isThrownBy {
          Original[0]
        }.withMessageContaining("Cannot call get from com.mkobit.gradle.test.kotlin.io.Original")
    }

    @DisplayName("CharSequence.subSequence(startIndex, endIndex) throws UnsupportedOperationException")
    @Test
    internal fun `CharSequence_subSequence throws UnsupportedOperationException`() {
      assertThatExceptionOfType(UnsupportedOperationException::class.java)
        .isThrownBy {
          Original.subSequence(0, 0)
        }.withMessageContaining("Cannot call subSequence from com.mkobit.gradle.test.kotlin.io.Original")
    }
  }

  @TestFactory internal fun `file attributes`(@TempDirectory.TempDir root: Path): Stream<DynamicNode> {
    val instant = Instant.from(
        LocalDateTime.of(2011, Month.NOVEMBER, 26, 7, 2)
            .atZone(ZoneId.systemDefault())
    )
    val clock = Clock.fixed(instant, ZoneId.systemDefault())
    return Stream.of(
        dynamicTest("regular file modification time") {
          val context = FileContext.RegularFileContext(root.newFile("fileModTime"))
          assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        dynamicTest("directory modification time") {
          val context = FileContext.DirectoryContext(root.newDirectory("dirModTime"))
          assertThat(context.lastModifiedTime)
              .isNotNull()
          context.lastModifiedTime = clock.instant()
          assertThat(context.lastModifiedTime)
              .isEqualTo(instant)
        },
        // TODO: make a cross-platform test here to make sure true and false can both be tested
        dynamicTest("regular file hidden status") {
          val context = FileContext.RegularFileContext(root.newFile("notHiddenFile"))
          assertThat(context.isHidden)
              .isFalse()
        },
        dynamicTest("directory hidden status") {
          val context = FileContext.DirectoryContext(root.newDirectory("notHiddenDir"))
          assertThat(context.isHidden)
              .isFalse()
        },
        dynamicTest("regular file size") {
          val bytes = 10
          val context = FileContext.RegularFileContext(root.newFile("fileSize", ByteArray(bytes, Int::toByte)))
          assertThat(context.size)
              .isEqualTo(bytes.toLong())
              .isEqualTo(context.content.size.toLong())
        }
    )
  }

  @Nested inner class RegularFileContextTest {
    private lateinit var fileContext: FileContext.RegularFileContext

    @BeforeEach internal fun setUp(@TempDirectory.TempDir root: Path, testInfo: TestInfo) {
      fileContext = FileContext.RegularFileContext(Files.createFile(root.resolve(testInfo.displayName)))
    }

    @TestFactory internal fun `constructor validation`(
      @TempDirectory.TempDir tempDir: Path
    ): Stream<DynamicNode> {
      val directory: Path = Files.createDirectory(tempDir.resolve("tempDir"))
      val regularFile: Path = Files.createFile(tempDir.resolve("tempFile"))
      val doesntExist = directory.resolve("dontexist")

      return Stream.of(
          dynamicTest("constructed with nonexistent file throws an IllegalArgumentException") {
            assertThatIllegalArgumentException().isThrownBy { FileContext.RegularFileContext(doesntExist) }
          },
          dynamicTest("constructed with directory throws an IllegalArgumentException") {
            assertThatIllegalArgumentException().isThrownBy {
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

    @Test internal fun `empty content can be read`() {
      assertThat(fileContext.content)
          .isEmpty()
    }

    @Test internal fun `content can be read`() {
      val content = "here is some file content".toByteArray()
      Files.write(fileContext.path, content)
      assertThat(fileContext.content)
          .isEqualTo(content)
    }

    @Test internal fun `empty content can be written`() {
      fileContext.content = ByteArray(0)
      assertThat(fileContext.content)
          .isEmpty()
    }

    @Test internal fun `entire contents can be written`() {
      val content = "this is some file content"
      fileContext.content = content.toByteArray()
      assertThat(fileContext.content)
          .isEqualTo(content.toByteArray())
      assertThat(fileContext.path)
          .hasContent(content)
    }

    @Test internal fun `append byte array content to existing file`() {
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

    @Test internal fun `append CharSequence content to existing file`() {
      val originalContent = "this is the the original content"
      val appendedContent = "this is the appended content"
      fileContext.content = originalContent.toByteArray()
      fileContext.append(appendedContent)
      assertThat(fileContext.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*appendedContent.toByteArray())
      assertThat(fileContext.path)
          .hasContent(originalContent + appendedContent)
    }

    @Test internal fun `append newline to existing file`() {
      val originalContent = "this is the the original content"
      fileContext.content = originalContent.toByteArray()
      fileContext.appendNewline()
      assertThat(fileContext.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*System.lineSeparator().toByteArray())
      assertThat(fileContext.path)
          .hasContent(originalContent + System.lineSeparator())
    }

    @Test internal fun `replace lines in file`() {
      val originalContent = """
        line 1
        line 2
        line 3
        """.trimIndent()
      fileContext.content = originalContent.toByteArray()
      fileContext.replaceEachLine { lineNumber, text ->
        when {
          text == "line 2" -> "LINE 2"
          lineNumber == 1 -> "First Line"
          else -> Original
        }
      }
      assertThat(fileContext.path)
          .hasContent("""
            First Line
            LINE 2
            line 3
          """.trimIndent())
    }
  }

  @Nested inner class DirectoryContextTest {

    private lateinit var directoryContext: FileContext.DirectoryContext

    @BeforeEach internal fun setUp(@TempDirectory.TempDir root: Path) {
      directoryContext = FileContext.DirectoryContext(root)
    }

    @TestFactory internal fun `constructor validation`(
      @TempDirectory.TempDir tempDir: Path
    ): Stream<DynamicNode> {
      val directory: Path = Files.createDirectory(tempDir.resolve("tempDir"))
      val regularFile: Path = Files.createFile(tempDir.resolve("tempFile"))
      val doesntExist = directory.resolve("dontexist")
      val symlink = Files.createSymbolicLink(directory.resolve("symlinkDestination"),
          Files.createFile(directory.resolve("symlinkSource")))
      return Stream.of(
          dynamicTest("constructed with nonexistent file throws IllegalArgumentException") {
            assertThatIllegalArgumentException()
                .isThrownBy { FileContext.DirectoryContext(doesntExist) }
          },
          dynamicTest("constructed with regular file throws IllegalArgumentException") {
            assertThatIllegalArgumentException()
                .isThrownBy { FileContext.DirectoryContext(regularFile) }
          },
          dynamicTest("constructed with symlink throws IllegalArgumentException") {
            assertThatIllegalArgumentException()
                .isThrownBy { FileContext.DirectoryContext(symlink) }
          },
          dynamicTest("constructed with existing directory does not throw an exception") {
            assertThatCode { FileContext.DirectoryContext(directory) }
                .doesNotThrowAnyException()
          }
      )
    }

    @Nested inner class FileActionMaybeCreate {
      private val requestType = FileAction.MaybeCreate

      @Test internal fun `when file does not exist and file is requested then it is created`() {
        val filename = "newFileName"
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .hasFileName(filename)
      }

      @Test internal fun `when file exists and file is requested then context is retrieved`() {
        val filename = "fileAlreadyExists"
        val filePath = directoryContext.path.newFile(filename)
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
            .isEqualTo(filePath)
      }

      @Test internal fun `when path is to a nonexistent file in an existing nested directory and file is requested then context is retrieved`() {
        val filename = "nonExistentFile"
        val dirPath = directoryContext.path.newDirectory("some/nested/dir")
        val fileContext = directoryContext.file("some/nested/dir/$filename", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(dirPath)
            .hasFileName(filename)
      }

      @Test internal fun `when path is a directory and file is requested then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("directory")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("directory", requestType) }
      }

      @Test internal fun `when path is a nested directory, the parent directory doesn't exist, and file is requested then an exception is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("directory/fileName", requestType) }
      }

      @Test internal fun `when the directory does not exist and directory is requested then it is created`() {
        val directoryName = "nonExistentDirectory"
        val context = directoryContext.directory(directoryName, requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName(directoryName)
      }

      @Test internal fun `when the nested directory does not exist and directory is requested then the entire path is created`() {
        val context = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test internal fun `when some of the nested directory does not exist then the entire path is created`() {
        directoryContext.path.newDirectory("path/to")
        val context = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test internal fun `when file exists at the path and directory is requested then FileAlreadyExistsException is thrown`() {
        directoryContext.path.newFile("regularFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("regularFile", requestType) }
      }

      @Disabled("temporary disable while reworking test structure")
      @Test internal fun `when nested file exists at the path and directory is requested then FileAlreadyExistsException is thrown`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("nested/dir/path"))
        val relative = directoryContext.path.relativize(file)
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory(relative.toString(), requestType) }
      }

      @TestFactory internal fun `when string invocation is called and directory does not exist then a file is created`(): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val filename = randomString()
              val context = directoryContext.run {
                filename(requestType) {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.RegularFileContext::class.java)
              assertThat(context.path)
                  .isRegularFile()
                  .hasParent(directoryContext.path)
                  .hasFileName(filename)
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val filename = randomString()
              val context = directoryContext.run {
                filename {}
              }
              assertThat(context)
                  .isInstanceOf(FileContext.RegularFileContext::class.java)
              assertThat(context.path)
                  .isRegularFile()
                  .hasParent(directoryContext.path)
                  .hasFileName(filename)
            }
        )
      }

      @Disabled("temporary disable while reworking test structure")
      @TestFactory internal fun `when string invocation is called and directory exists then FileAlreadyExistsException is thrown`(@TempDirectory.TempDir directory: Path): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              assertThatFileAlreadyExistsException()
                  .isThrownBy {
                    directoryContext.run {
                      directory.fileNameString(requestType) {}
                    }
                  }
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              assertThatFileAlreadyExistsException()
                  .isThrownBy {
                    directoryContext.run {
                      directory.fileNameString {}
                    }
                  }
            }
        )
      }

      @TestFactory internal fun `when string invocation is called, file already exists, and the provided content is Original, then the file is retrieved with its original content`(): Stream<DynamicNode> {
        val originalContent = "this is the original file content"
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val filename = randomString()
              directoryContext.path.newFile(filename, originalContent.toByteArray())
              val context = directoryContext.run {
                filename(requestType, content = Original)
              }
              assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(directoryContext.path)
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val filename = randomString()
              directoryContext.path.newFile(filename, originalContent.toByteArray())
              val context = directoryContext.run {
                filename(content = Original)
              }
              assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(directoryContext.path)
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName} and using default content parameter of Original") {
              val filename = randomString()
              directoryContext.path.newFile(filename, originalContent.toByteArray())
              val context = directoryContext.run {
                filename {
                }
              }
              assertThat(context).isInstanceOf(FileContext.RegularFileContext::class.java)
              assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(directoryContext.path)
            }
        )
      }

      @TestFactory internal fun `when string invocation is called with content and directory exists then FileAlreadyExistsException is thrown`(): Stream<DynamicNode> {
        val content = "this is file content"
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val filename = randomString()
              directoryContext.path.newDirectory(filename)
              directoryContext.run {
                assertThatFileAlreadyExistsException()
                    .isThrownBy {
                      filename(requestType, content = content)
                    }
              }
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val filename = randomString()
              directoryContext.path.newDirectory(filename)
              directoryContext.run {
                assertThatFileAlreadyExistsException()
                    .isThrownBy {
                      filename(content = content)
                    }
              }
            }
        )
      }

      @Disabled("temporary disable while reworking test structure")
      @Test internal fun `when div operator called and directory does not exist then it is created`() {
        val directoryName = randomString()
        val subdirContext = directoryContext / directoryName
        assertThat(subdirContext).isInstanceOf(FileContext.DirectoryContext::class.java)
        assertThat(subdirContext.path)
            .isDirectory()
            .hasFileName(directoryName)
            .hasParent(directoryContext.path)
      }

      @Disabled("temporary disable while reworking test structure")
      @Test internal fun `when div operator called and directory does exist then it is retrieved`(@TempDirectory.TempDir directory: Path) {
        val subdirContext = directoryContext / directory.fileNameString
        assertThat(subdirContext).isInstanceOf(FileContext.DirectoryContext::class.java)
        assertThat(subdirContext.path)
            .isDirectory()
            .endsWith(directory)
            .hasParent(directoryContext.path)
      }

      @Test internal fun `when div operator called and file exists at location then FileAlreadyExistsException is thrown `(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("file"))
        assertThatFileAlreadyExistsException().isThrownBy {
          directoryContext / file.fileNameString
        }
      }

      @Test
      internal fun `when div operator called with multiple paths then it is created`() {
        val childContext = directoryContext / "first" / "second" / "third"

        assertThat(childContext).isInstanceOf(FileContext.DirectoryContext::class.java)
        assertThat(childContext.path)
            .isDirectory()
            .endsWith(Paths.get("first", "second", "third"))
      }

      @Test
      internal fun `when div operator called with body then the body is applied to the instance`() {
        var invoked = false
        val result = directoryContext / {
          invoked = true
          assertThat(this)
              .isSameAs(directoryContext)
        }
        assertThat(invoked)
            .withFailMessage("Body was not invoked")
            .isTrue()
        assertThat(result)
            .describedAs("Body returns same instance of invocation")
            .isSameAs(directoryContext)
      }

      @Test
      internal fun `when div operator called with path and body then the body is applied to the path`() {
        var invoked = false
        var invokedResult: FileContext.DirectoryContext? = null
        val result = directoryContext / "childDir" / {
          invoked = true
          assertThat(this).isInstanceOf(FileContext.DirectoryContext::class.java)
          assertThat(path)
              .hasFileName("childDir")
              .isDirectory()
              .hasParent(directoryContext.path)
          invokedResult = this
        }
        assertThat(invoked)
            .withFailMessage("Body was not invoked")
            .isTrue()
        assertThat(result)
            .describedAs("Body returns receiver of body")
            .isSameAs(invokedResult)
      }
    }

    @Nested inner class FileActionCreate {
      private val requestType = FileAction.Create

      @Test internal fun `when the file does not exist and file is requested then it is created`() {
        val fileContext = directoryContext.file("nonExistentFile", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
      }

      @Test internal fun `when the path is nested and the parent directory doesn't exist then an exception is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("path/to/nonExistentDir/file", requestType) }
      }

      @Test internal fun `when the path is nested, the parent directory exists, and file is requested then the file is created`() {
        val directoryName = randomString()
        val dirPath = Files.createDirectories(directoryContext.path.resolve("path/to/$directoryName"))
        val context = directoryContext.file("path/to/$directoryName/newFile", requestType)
        assertThat(context.path)
            .hasParent(dirPath)
            .isRegularFile()
      }

      @Test fun `when the file exists and file is requested then a FileAlreadyExistsException is thrown`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("directory"))
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file(file.fileNameString, requestType) }
      }

      @Test internal fun `when the file exists in a nested directory then a FileAlreadyExistsException is thrown`(@TempDirectory.TempDir directory: Path) {
        val dirPath = Files.createDirectories(directoryContext.path.resolve("path/to/dir"))
        val filename = randomString()
        Files.createFile(dirPath.resolve(filename))
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("path/to/dir/$filename", requestType) }
      }

      @Test internal fun `when a directory already exists at the path and file is requested then a FileAlreadyExistsException is thrown`() {
        val directoryName = randomString()
        directoryContext.path.newDirectory(directoryName)
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file(directoryName, requestType) }
      }

      @Test internal fun `when the directory does not exist then then it is created`() {
        val directoryName = randomString()
        val context = directoryContext.directory(directoryName, requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName(directoryName)
      }

      @Test internal fun `when the path is nested and the nested directory doesn't exist then the nested path is created`() {
        val directoryName = "nested/dir/path"
        val context = directoryContext.directory(directoryName, requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get(directoryName))
      }

      @Test internal fun `when the path is nested and some of the parent directories exist and directory is requested then the full nested directory is created`(@TempDirectory.TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("nested/dir"))
        val directoryName = directoryContext.path.relativize(directory.parent.resolve(randomString()))
        val context = directoryContext.directory(directoryName.toString(), requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(directoryName)
      }

      @Disabled("temporary disable while reworking test structure")
      @Test internal fun `when the directory exists and directory is requested then an exception is thrown`(@TempDirectory.TempDir directory: Path) {
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory(directory.fileNameString, requestType) }
      }

      @Test internal fun `when the directory exists and directory is requested in a nested directory then an exception is thrown`(@TempDirectory.TempDir directory: Path) {
        val nestedDirectory = Files.createDirectories(directory.resolve("nested/dir"))
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory(directoryContext.path.relativize(nestedDirectory).toString(), requestType) }
      }

      @Test internal fun `when a file exists and directory is requested then an exception is thrown`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("file.txt"))
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory(file.fileNameString, requestType) }
      }
    }

    @Nested inner class FileActionGet {
      private val requestType = FileAction.Get

      @Test internal fun `when file does not exist then an exception is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("doesntExist", requestType) }
      }

      @Test internal fun `when file exists directly in the directory then directoryContext can be retrieved`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("file"))
        val fileContext = directoryContext.file(file.fileNameString, requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .isEqualTo(file)
      }

      @Test internal fun `when file exists in a nested directory then directoryContext can be retrieved`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(Files.createDirectory(directory.resolve("nested")).resolve("file.txt"))
        val filePath = directoryContext.path.relativize(file)
        val fileContext = directoryContext.file(filePath.toString(), requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .endsWith(filePath)
      }

      @Test internal fun `when path is a directory then an exception is thrown`(@TempDirectory.TempDir directory: Path) {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file(directory.fileNameString, requestType) }
      }

      @Test internal fun `when directory does not exist and directory is requested then a NoSuchFileException is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.directory("nonExistentDirectory", requestType) }
      }

      @Disabled("temporary disable while reworking test structure")
      @Test internal fun `when direct child directory exists then directoryContext can be retrieved`(@TempDirectory.TempDir directory: Path) {
        val context = directoryContext.directory(directory.fileNameString, requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test internal fun `when nested directory exists and directory is requested then it is retrieved`(@TempDirectory.TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("nested/dir"))
        val relative = directoryContext.path.relativize(directory)
        val context = directoryContext.directory(relative.toString(), requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test internal fun `when file exists and directory is requested then a NoSuchFileException is thrown`(@TempDirectory.TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("file"))
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.directory(file.fileNameString, requestType) }
      }
    }
  }

  @TestFactory internal fun `FileRequest types`(): Stream<DynamicNode> {
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

private fun randomString(): String = UUID.randomUUID().toString()
