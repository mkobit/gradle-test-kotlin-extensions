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
import org.junit.jupiter.api.io.TempDir
import testsupport.assertThatFileAlreadyExistsException
import testsupport.assertThatNoSuchFileException
import testsupport.fileNameString
import testsupport.newDirectory
import testsupport.newFile
import java.nio.file.Files
import java.nio.file.LinkOption
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

  @Suppress("UNUSED")
  private interface FileAttributesTests {
    val context: FileContext

    @Test fun `can get and set last modified time`() {
      val instant = Instant.from(
        LocalDateTime.of(2011, Month.NOVEMBER, 26, 7, 2)
          .atZone(ZoneId.systemDefault())
      )
      val clock = Clock.fixed(instant, ZoneId.systemDefault())

      assertThat(context.lastModifiedTime)
        .isNotNull()
      context.lastModifiedTime = clock.instant()
      assertThat(context.lastModifiedTime)
        .isEqualTo(instant)
    }

    @Test fun `can get hidden status`() {
      // TODO: make a cross-platform test here to make sure true and false can both be tested
      assertThat(context.isHidden)
        .isFalse()
    }

    @Disabled("make a cross-platform test here to make sure true and false can both be tested")
    @Test fun `can set hidden status`() {
    }

    @Test fun `can get file owner`() {
      assertThatCode {
        context.owner
      }.doesNotThrowAnyException()
    }

    @Disabled("need to come up with a safe way to test this - see something like https://stackoverflow.com/questions/13241967/change-file-owner-group-under-linux-with-java-nio-files")
    @Test fun `can set file owner`() {
    }

    @Test fun `can read POSIX attributes`() {
      assertThat(context.posixFilePermissions)
        .isNotEmpty
    }

    @Test fun `can set POSIX attributes`() {
      val old = context.posixFilePermissions
      val new = old + PosixFilePermission.OWNER_EXECUTE - PosixFilePermission.GROUP_READ - PosixFilePermission.OTHERS_READ
      context.posixFilePermissions = new
      assertThat(Files.getPosixFilePermissions(context.path, LinkOption.NOFOLLOW_LINKS))
        .isEqualTo(new)
    }
  }

  @Nested inner class RegularFileContextTest : FileAttributesTests {
    private lateinit var fileContext: FileContext.RegularFileContext

    override val context: FileContext
      get() = fileContext

    @BeforeEach internal fun setUp(@TempDir root: Path, testInfo: TestInfo) {
      fileContext = FileContext.RegularFileContext(Files.createFile(root.resolve(testInfo.displayName)))
    }

    @TestFactory internal fun `constructor validation`(
      @TempDir tempDir: Path
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

    @Test internal fun `can get the file size in bytes`() {
      val bytes = 10
      fileContext.content = ByteArray(bytes, Int::toByte)
      assertThat(fileContext.size)
        .isEqualTo(bytes.toLong())
        .isEqualTo(fileContext.content.size.toLong())
    }

    @Test internal fun `empty content can be read`() {
      assertThat(fileContext.content)
          .isEmpty()
    }

    @Test internal fun `ByteArray content can be written and read`() {
      val content = "here is some file content".toByteArray()
      Files.write(fileContext.path, content)
      assertThat(fileContext.content)
          .isEqualTo(content)
    }

    @Test internal fun `empty ByteArray content can be written and read`() {
      fileContext.content = ByteArray(0)
      assertThat(fileContext.content)
          .isEmpty()
    }

    @Test internal fun `append ByteArray content to existing file`() {
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

  @Nested inner class DirectoryContextTest : FileAttributesTests {

    private lateinit var directoryContext: FileContext.DirectoryContext

    override val context: FileContext
      get() = directoryContext

    @BeforeEach internal fun setUp(@TempDir directory: Path) {
      directoryContext = FileContext.DirectoryContext(directory)
    }

    @TestFactory internal fun `constructor validation`(
      @TempDir tempDir: Path
    ): Stream<DynamicNode> {
      val directory: Path = Files.createDirectory(tempDir.resolve("tempDir"))
      val regularFile: Path = Files.createFile(tempDir.resolve("tempFile"))
      val doesntExist = directory.resolve("dontexist")
      val symlink = Files.createSymbolicLink(directory.resolve("newlink"),
          Files.createFile(directory.resolve("linkTarget")))
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

      @Test internal fun `when a file is requested and file does not exist then it is created`() {
        val filename = "newFileName"
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .hasFileName(filename)
      }

      @Test internal fun `when a file is requested and file exists then it is retrieved`() {
        val filename = "fileAlreadyExists"
        val filePath = directoryContext.path.newFile(filename)
        val fileContext = directoryContext.file(filename, requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
            .isEqualTo(filePath)
      }

      @Test internal fun `when a nonexistent file in an existing nested directory requested then it is created`() {
        val directory = "some/nested/dir"
        val filename = "nonExistentFile"
        val dirPath = directoryContext.path.newDirectory(directory)
        val fileContext: FileContext.RegularFileContext = directoryContext.file("$directory/$filename", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(dirPath)
            .hasFileName(filename)
      }

      @Test internal fun `when a file is requested and directory exists then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("directory")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("directory", requestType) }
      }

      @Test internal fun `when a nested file is requested and the parent directory doesn't exist then a NoSuchFileException is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("directory/fileName", requestType) }
      }

      @Test internal fun `when a directory is requested and the directory does not exist then it is created`() {
        val directoryName = "nonExistentDirectory"
        val context: FileContext.DirectoryContext = directoryContext.directory(directoryName, requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName(directoryName)
      }

      @Test internal fun `when a nested directory is requested and a portion of the path exists then the rest of the path is created`() {
        val context: FileContext.DirectoryContext = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test internal fun `when a nested directory is requested and some of the parent path does not exist then the rest is created`() {
        directoryContext.path.newDirectory("path/to")
        val context: FileContext.DirectoryContext = directoryContext.directory("path/to/nonexistent/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("path/to/nonexistent/dir"))
      }

      @Test internal fun `when a directory is requested and file exists at the path then FileAlreadyExistsException is thrown`() {
        directoryContext.path.newFile("regularFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("regularFile", requestType) }
      }

      @Test internal fun `when a nested directory is requested and file exists at the path then FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("path/dir/path").newFile("actuallyAFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("path/dir/path/actuallyAFile", requestType) }
      }

      @TestFactory internal fun `when string invocation is called and directory does not exist then the file is created`(): Stream<DynamicNode> {
        return Stream.of(
            dynamicTest("by explicitly passing in a ${FileAction::class.simpleName} of ${requestType::class.simpleName}") {
              val filename = randomString()
              val context: FileContext.RegularFileContext = directoryContext.run {
                filename(requestType) {}
              }
              assertThat(context.path)
                  .isRegularFile()
                  .hasParent(directoryContext.path)
                  .hasFileName(filename)
            },
            dynamicTest("using default parameter value of ${FileAction::class.simpleName}") {
              val filename = randomString()
              val context: FileContext.RegularFileContext = directoryContext.run {
                filename {}
              }
              assertThat(context.path)
                  .isRegularFile()
                  .hasParent(directoryContext.path)
                  .hasFileName(filename)
            }
        )
      }

      @TestFactory internal fun `when string invocation is called and directory exists then FileAlreadyExistsException is thrown`(): Stream<DynamicNode> {
        val directory = directoryContext.path.newDirectory("preExistingDir")
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

      @TestFactory internal fun `when string invocation is called with content and directory exists at path then FileAlreadyExistsException is thrown`(): Stream<DynamicNode> {
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

      @Test internal fun `when div invocation is called and directory does not exist then it is created`() {
        val subdirContext: FileContext.DirectoryContext = directoryContext / "nonExistentSubDirectory"
        assertThat(subdirContext.path)
            .isDirectory()
            .hasFileName("nonExistentSubDirectory")
            .hasParent(directoryContext.path)
      }

      @Test internal fun `when div invocation is called and directory does exist then it is retrieved`() {
        val subDirectory = directoryContext.path.newDirectory("preExistingSubDirectory")
        val subdirContext: FileContext.DirectoryContext = directoryContext / subDirectory.fileNameString
        assertThat(subdirContext.path)
            .isDirectory()
            .endsWith(subDirectory)
            .hasParent(directoryContext.path)
      }

      @Test internal fun `when div invocation is called and file exists at the location then a FileAlreadyExistsException is thrown`() {
        val file = directoryContext.path.newFile("preExistingFile")
        assertThatFileAlreadyExistsException().isThrownBy {
          directoryContext / file.fileNameString
        }
      }

      @Test internal fun `when div invocation is successively called with multiple paths then the entire directory path is created`() {
        val childContext: FileContext.DirectoryContext = directoryContext / "first" / "second" / "third"

        assertThat(childContext.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("first", "second", "third"))
      }

      @Test internal fun `when div invocation is called with lambda then the lambda is called with the same instance as the receiver`() {
        var invoked = false
        val result: FileContext.DirectoryContext = directoryContext / {
          invoked = true
          assertThat(this)
              .isSameAs(directoryContext)
        }
        assertThat(invoked)
            .withFailMessage("Lambda body was not invoked")
            .isTrue()
        assertThat(result)
            .describedAs("Operator invocation returns same instance that is receiver of div div operation")
            .isSameAs(directoryContext)
      }

      @Test internal fun `when div invocation is called with path and lambda then the lambda is applied to the receiver that is at the path`() {
        var invoked = false
        var thisFromLambdaBodyOfDivInvoke: FileContext.DirectoryContext? = null
        val result: FileContext.DirectoryContext = directoryContext / "childDir" / {
          invoked = true
          assertThat(this).isInstanceOf(FileContext.DirectoryContext::class.java)
          assertThat(path)
              .hasFileName("childDir")
              .isDirectory()
              .hasParent(directoryContext.path)
          thisFromLambdaBodyOfDivInvoke = this
        }
        assertThat(invoked)
            .withFailMessage("Lambda body was not invoked")
            .isTrue()
        assertThat(result)
            .describedAs("Operator invocation returns same instance that is receiver of div div operation")
            .isSameAs(thisFromLambdaBodyOfDivInvoke)
      }
    }

    @Nested inner class FileActionCreate {
      private val requestType = FileAction.Create

      @Test internal fun `when a file is requested and the file does not exist then it is created`() {
        val fileContext: FileContext.RegularFileContext = directoryContext.file("nonExistentFile", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .hasParent(directoryContext.path)
      }

      @Test internal fun `when a nested file is requested and the parent directory doesn't exist then a NoSuchFileException is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("path/to/nonExistentDir/file", requestType) }
      }

      @Test internal fun `when a nested file is requested and the the parent directory exists then the file is created`() {
        val dirPath = directoryContext.path.newDirectory("path/to/dir")
        val context: FileContext.RegularFileContext = directoryContext.file("path/to/dir/newFile", requestType)
        assertThat(context.path)
            .hasParent(dirPath)
            .isRegularFile()
            .endsWith(Paths.get("newFile"))
      }

      @Test fun `when a file is requested and the file exists then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newFile("preExistingFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("preExistingFile", requestType) }
      }

      @Test internal fun `when a nested file is requested and a file already exists then a FileAlreadyExistsException is thrown`() {
        val file = directoryContext.path.newDirectory("path/to/dir").newFile("preExistingFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("path/to/dir/${file.fileNameString}", requestType) }
      }

      @Test internal fun `when a nested file is requested and a directory already exists then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("path/to/dir")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.file("path/to/dir", requestType) }
      }

      @Test internal fun `when a directory is requested and the directory doesn't exist then then it is created`() {
        val context: FileContext.DirectoryContext = directoryContext.directory("nonExistentDir", requestType)
        assertThat(context.path)
            .isDirectory()
            .hasParent(directoryContext.path)
            .hasFileName("nonExistentDir")
      }

      @Test internal fun `when a nested directory is requested and the nested directory doesn't exist then the nested path is created`() {
        val context: FileContext.DirectoryContext = directoryContext.directory("nested/dir/path", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(directoryContext.path)
            .endsWith(Paths.get("nested/dir/path"))
      }

      @Test internal fun `when a nested directory is requested and a portion of the path exists then the rest of the path is created`() {
        val directory = directoryContext.path.newDirectory("nested/dir")
        val context: FileContext.DirectoryContext = directoryContext.directory("nested/dir/farther/nesting", requestType)
        assertThat(context.path)
            .isDirectory()
            .startsWith(context.path)
            .startsWith(directory)
            .endsWith(Paths.get("farther", "nesting"))
      }

      @Test internal fun `when a directory is requested and the directory exists then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("preExistingDirectory")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("preExistingDirectory", requestType) }
      }

      @Test internal fun `when a nested directory is requested and the nested directory exists then a FileAlreadyExistsException is thrown`() {
        directoryContext.path.newDirectory("pre/existing/nested/dir")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("pre/existing/nested/dir", requestType) }
      }

      @Test internal fun `when a directory is requested and file already exists then a FileAlreadyExistsException is thrown`(@TempDir directory: Path) {
        directoryContext.path.newFile("preExistingFile")
        assertThatFileAlreadyExistsException()
            .isThrownBy { directoryContext.directory("preExistingFile", requestType) }
      }
    }

    @Nested inner class FileActionGet {
      private val requestType = FileAction.Get

      @Test internal fun `when a file does not exist then a NoSuchFileException is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("nonExistentFile", requestType) }
      }

      @Test internal fun `when a file is requested and the file exists then the context is retrieved`() {
        val file = directoryContext.path.newFile("preExistingFile")
        val fileContext: FileContext.RegularFileContext = directoryContext.file(file.fileNameString, requestType)
        assertThat(fileContext.path)
            .hasParent(directoryContext.path)
            .isRegularFile()
            .isEqualTo(file)
      }

      @Test internal fun `when a nested file is requested and the file exists then the context is retrieved`() {
        val file = directoryContext.path.newDirectory("path/to/dir").newFile("preExistingFile")
        val fileContext: FileContext.RegularFileContext = directoryContext.file("path/to/dir/preExistingFile", requestType)
        assertThat(fileContext.path)
            .isRegularFile()
            .startsWith(directoryContext.path)
            .endsWith(file)
      }

      @Test internal fun `when a file is requested and the file is a directory then a NoSuchFileException is thrown`() {
        directoryContext.path.newDirectory("preExistingDir")
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.file("preExistingDir", requestType) }
      }

      @Test internal fun `when a directory is requested and the directory does not exist then a NoSuchFileException is thrown`() {
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.directory("nonExistentDirectory", requestType) }
      }

      @Test internal fun `when directory is requested and the directory exists then the context is retrieved`() {
        val directory = directoryContext.path.newDirectory("preExistingDirectory")
        val context: FileContext.DirectoryContext = directoryContext.directory("preExistingDirectory", requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test internal fun `when nested directory is requested and the directory exists then the context is retrieved`() {
        val directory = directoryContext.path.newDirectory("nested/pre/existing/dir")
        val context: FileContext.DirectoryContext = directoryContext.directory("nested/pre/existing/dir", requestType)
        assertThat(context.path)
            .isDirectory()
            .isEqualTo(directory)
      }

      @Test internal fun `when directory is requested and file exists then a NoSuchFileException is thrown`() {
        val file = directoryContext.path.newFile("preExistingFile")
        assertThatNoSuchFileException()
            .isThrownBy { directoryContext.directory("preExistingFile", requestType) }
      }
    }
  }

  @Nested inner class FileRequest {
    private val posixFilePermissions = PosixFilePermissions.asFileAttribute(
      setOf(
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.GROUP_READ
      )
    )

    @Test
    internal fun `Get type is an object`() {
      assertThat(FileAction.Get::class.objectInstance)
        .withFailMessage("${FileAction.Get::class} must be an object instance")
        .isNotNull
    }

    @Test
    internal fun `MaybeCreate is a constant`() {
      assertThat(FileAction.MaybeCreate)
        .isInstanceOf(FileAction.MaybeCreate::class.java)
    }

    @Test
    internal fun `MaybeCreate instance with attributes`() {
      val request = FileAction.MaybeCreate(listOf(posixFilePermissions))
      assertThat(request.fileAttributes)
        .containsExactly(posixFilePermissions)
    }

    @Test
    internal fun `Create is a constant`() {
      assertThat(FileAction.Create)
        .isInstanceOf(FileAction.Create::class.java)
    }

    @Test
    internal fun `Create instance with attributes`() {
      val request = FileAction.Create(listOf(posixFilePermissions))
      assertThat(request.fileAttributes)
        .containsExactly(posixFilePermissions)
    }
  }
}

private fun randomString(): String = UUID.randomUUID().toString()
