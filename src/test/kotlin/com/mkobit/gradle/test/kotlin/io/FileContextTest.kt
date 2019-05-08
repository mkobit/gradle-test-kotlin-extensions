package com.mkobit.gradle.test.kotlin.io

import dev.minutest.ContextBuilder
import dev.minutest.experimental.SKIP
import dev.minutest.experimental.minus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.message
import testsupport.assertj.assertNoExceptionThrownBy
import testsupport.jdk.fileNameString
import testsupport.minutest.createDirectoriesFor
import testsupport.minutest.createFileFor
import testsupport.minutest.testFactory
import testsupport.jdk.newDirectory
import testsupport.jdk.newFile
import testsupport.strikt.isEmpty
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

internal class FileContextTest {

  private fun ContextBuilder<out FileContext>.fileAttributesTests() {
    // TODO: better context groupings
    // needed for https://github.com/dmcg/minutest/issues/28
    derivedContext<FileContext>("casted to FileContext (see https://github.com/dmcg/minutest/issues/28)") {
      deriveFixture { this }
      test("can get and set last modified time") {
        val instant = Instant.from(
          LocalDateTime.of(2011, Month.NOVEMBER, 26, 7, 2)
            .atZone(ZoneId.systemDefault())
        )
        val clock = Clock.fixed(instant, ZoneId.systemDefault())

        assertThat(fixture.lastModifiedTime)
          .isNotNull()
        fixture.lastModifiedTime = clock.instant()
        assertThat(fixture.lastModifiedTime)
          .isEqualTo(instant)
      }

      test("can get hidden status") {
        // TODO: make a cross-platform test here to make sure true and false can both be tested
        assertThat(fixture.isHidden).isFalse()
      }

      SKIP - test("can set hidden status") {
        fail("make a cross-platform test here to make sure true and false can both be tested")
      }

      test("can get file owner") {
        assertNoExceptionThrownBy { fixture.owner }
      }

      SKIP - test("can set file owner") {
        fail("need to come up with a safe way to test this - see something like https://stackoverflow.com/questions/13241967/change-file-owner-group-under-linux-with-java-nio-files")
      }

      test("can read POSIX attributes") {
        assertThat(fixture.posixFilePermissions)
          .isNotEmpty
      }

      test("can set POSIX attributes") {
        val old = fixture.posixFilePermissions
        val new = old + PosixFilePermission.OWNER_EXECUTE - PosixFilePermission.GROUP_READ - PosixFilePermission.OTHERS_READ
        fixture.posixFilePermissions = new
        assertThat(Files.getPosixFilePermissions(fixture.path, LinkOption.NOFOLLOW_LINKS))
          .isEqualTo(new)
      }
    }
  }

  @TestFactory
  internal fun original() = testFactory<Unit> {
    test("Original is a singleton object") {
      expectThat(Original)
        .get { this::class }
        .get { objectInstance }
        .describedAs("Is object instance")
        .isNotNull()
    }

    test("CharSequence_length throws UnsupportedOperationException") {
      expectThrows<UnsupportedOperationException> {
          Original.length
        }.message.contains("Cannot access length from com.mkobit.gradle.test.kotlin.io.Original")
    }

    test("CharSequence_get throws UnsupportedOperationException") {
      expectThrows<UnsupportedOperationException> {
          Original[0]
        }.message.contains("Cannot call get from com.mkobit.gradle.test.kotlin.io.Original")
    }

    test("CharSequence_subSequence throws UnsupportedOperationException") {
      expectThrows<UnsupportedOperationException> {
          Original.subSequence(0, 0)
        }.message.contains("Cannot call subSequence from com.mkobit.gradle.test.kotlin.io.Original")
    }
  }

  @TestFactory
  internal fun `regular file context`(@TempDir directory: Path) = testFactory<Unit> {
    derivedContext<Path>("when constructed with") {
      fixture { directory.createDirectoriesFor(it) }

      context("a regular file") {
        deriveFixture { Files.createFile(fixture.resolve("regular-file")) }
        test("then no exception is thrown") {
          assertNoExceptionThrownBy { FileContext.RegularFileContext(fixture) }
        }
      }
      context("a nonexistent file") {
        deriveFixture { fixture.resolve("nonexistent") }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.RegularFileContext(fixture) }
        }
      }
      context("when constructed with a directory") {
        deriveFixture { Files.createDirectory(fixture.resolve("directory")) }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.RegularFileContext(fixture) }
        }
      }
      context("a symlink") {
        deriveFixture {
          Files.createSymbolicLink(
            fixture.resolve("newlink"),
            Files.createFile(fixture.resolve("link-target"))
          )
        }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.RegularFileContext(fixture) }
        }
      }
    }

    derivedContext<FileContext.RegularFileContext>("a FileContext.RegularFileContext") {
      fixture { FileContext.RegularFileContext(directory.createFileFor(it)) }

      fileAttributesTests()

      test("can get the file size in bytes") {
        val bytes = 10
        fixture.content = ByteArray(bytes, Int::toByte)
        assertThat(fixture.size)
          .isEqualTo(bytes.toLong())
          .isEqualTo(fixture.content.size.toLong())
      }

      test("empty content can be read") {
        assertThat(fixture.content)
          .isEmpty()
      }

      test("ByteArray content can be written and read") {
        val content = "here is some file content".toByteArray()
        Files.write(fixture.path, content)
        expectThat(fixture)
          .get { content }
          .isEqualTo(content)
      }

      test("empty ByteArray content can be written and read") {
        fixture.content = ByteArray(0)
        expectThat(fixture)
          .get { content }
          .isEmpty()
      }

      test("append ByteArray content to existing file") {
        val originalContent = "this is the the original content"
        val appendedContent = "this is the appended content"
        fixture.content = originalContent.toByteArray()
        fixture.append(appendedContent.toByteArray())
        assertThat(fixture.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*appendedContent.toByteArray())
        assertThat(fixture.path)
          .hasContent(originalContent + appendedContent)
      }

      test("append CharSequence content to existing file") {
        val originalContent = "this is the the original content"
        val appendedContent = "this is the appended content"
        fixture.content = originalContent.toByteArray()
        fixture.append(appendedContent)
        assertThat(fixture.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*appendedContent.toByteArray())
        assertThat(fixture.path)
          .hasContent(originalContent + appendedContent)
      }

      test("append newline to existing file") {
        val originalContent = "this is the the original content"
        fixture.content = originalContent.toByteArray()
        fixture.appendNewline()
        assertThat(fixture.content)
          .startsWith(*originalContent.toByteArray())
          .endsWith(*System.lineSeparator().toByteArray())
        assertThat(fixture.path)
          .hasContent(originalContent + System.lineSeparator())
      }

      test("replace lines in file") {
        val originalContent = """
        line 1
        line 2
        line 3
        """.trimIndent()
        fixture.content = originalContent.toByteArray()
        fixture.replaceEachLine { lineNumber, text ->
          when {
            text == "line 2" -> "LINE 2"
            lineNumber == 1 -> "First Line"
            else -> Original
          }
        }
        assertThat(fixture.path)
          .hasContent("""
            First Line
            LINE 2
            line 3
          """.trimIndent())
      }
    }
  }

  @TestFactory
  internal fun `directory context`(@TempDir directory: Path) = testFactory<Unit> {
    derivedContext<Path>("when constructed with") {
      fixture { directory.createDirectoriesFor(it) }

      context("a regular file") {
        deriveFixture { Files.createFile(fixture.resolve("regular-file")) }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.DirectoryContext(fixture) }
        }
      }
      context("a nonexistent file") {
        deriveFixture { fixture.resolve("nonexistent") }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.DirectoryContext(fixture) }
        }
      }
      context("a directory") {
        deriveFixture { Files.createDirectory(fixture.resolve("directory")) }
        test("then no exception is thrown") {
          assertNoExceptionThrownBy { FileContext.DirectoryContext(fixture) }
        }
      }
      context("a symlink") {
        deriveFixture {
          Files.createSymbolicLink(
            fixture.resolve("newlink"),
            Files.createDirectory(fixture.resolve("link-target"))
          )
        }
        test("then an illegal argument exception is thrown") {
          expectThrows<IllegalArgumentException> { FileContext.DirectoryContext(fixture) }
        }
      }
    }

    derivedContext<FileContext.DirectoryContext>("a FileContext.DirectoryContext") {
      fixture { FileContext.DirectoryContext(directory.createDirectoriesFor(it)) }
      fileAttributesTests()
      context("execute file action") {
        class FileActionFixture(val fileContext: FileContext.DirectoryContext, val action: FileAction)
        derivedContext<FileActionFixture>("is MaybeCreate") {
          deriveFixture { FileActionFixture(parentFixture, FileAction.MaybeCreate) }
          test("when a file is requested and file does not exist then it is created") {
            val filename = "new-file-name"
            val fileContext = fileContext.file(filename, action)
            assertThat(fileContext.path)
              .hasParent(this.fileContext.path)
              .isRegularFile()
              .hasFileName(filename)
          }

          test("when a file is requested and file exists then it is retrieved") {
            val filename = "fileAlreadyExists"
            val filePath = fileContext.path.newFile(filename)
            val fileContext = fileContext.file(filename, action)
            assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(this.fileContext.path)
              .isEqualTo(filePath)
          }

          test("when a nonexistent file in an existing nested directory requested then it is created") {
            val nestedDirectory = "some/nested/dir"
            val filename = "nonExistentFile"
            val dirPath = fileContext.path.newDirectory(nestedDirectory)
            val fileContext: FileContext.RegularFileContext = fileContext.file("$nestedDirectory/$filename", action)
            assertThat(fileContext.path)
              .isRegularFile()
              .hasParent(dirPath)
              .hasFileName(filename)
          }

          test("when a file is requested and directory exists then a FileAlreadyExistsException is thrown") {
            fileContext.path.newDirectory("directory")
            expectThrows<FileAlreadyExistsException> { fileContext.file("directory", action) }
          }

          test("when a nested file is requested and the parent directory doesn't exist then a NoSuchFileExceptiis thrown") {
            expectThrows<NoSuchFileException> { fileContext.file("directory/fileName", action) }
          }

          test("when a directory is requested and the directory does not exist then it is created") {
            val directoryName = "non-existent-directory"
            val context: FileContext.DirectoryContext = fileContext.directory(directoryName, action)
            assertThat(context.path)
              .isDirectory()
              .hasParent(fileContext.path)
              .hasFileName(directoryName)
          }

          test("when a nested directory is requested and a portion of the path exists then the rest of the path is created") {
            val context: FileContext.DirectoryContext = fileContext.directory("path/to/nonexistent/dir", action)
            assertThat(context.path)
              .isDirectory()
              .startsWith(fileContext.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
          }

          test("when a nested directory is requested and some of the parent path does not exist then the rest is created") {
            fileContext.path.newDirectory("path/to")
            val context: FileContext.DirectoryContext = fileContext.directory("path/to/nonexistent/dir", action)
            assertThat(context.path)
              .isDirectory()
              .startsWith(fileContext.path)
              .endsWith(Paths.get("path/to/nonexistent/dir"))
          }

          test("when a directory is requested and file exists at the path then FileAlreadyExistsException is thrown") {
            fileContext.path.newFile("regularFile")
            expectThrows<FileAlreadyExistsException> {
              fileContext.directory("regularFile",
                action)
            }
          }

          test("when a nested directory is requested and file exists at the path then FileAlreadyExistsException is thrown") {
            fileContext.path.newDirectory("path/dir/path").newFile("actuallyAFile")
            expectThrows<FileAlreadyExistsException> {
              fileContext.directory("path/dir/path/actuallyAFile",
                action)
            }
          }
          context("when string invocation is used") {
            context("and a directory does not exist at the path") {
              val filename = "new-file-to-create"
              test("then the file is created by explicitly passing in the action") {
                val context: FileContext.RegularFileContext = fileContext.run {
                  filename(action) {}
                }
                assertThat(context.path)
                  .isRegularFile()
                  .hasParent(fileContext.path)
                  .hasFileName(filename)
              }
              test("then the file is created by the using the default action") {
                val context: FileContext.RegularFileContext = fileContext.run {
                  filename {}
                }
                assertThat(context.path)
                  .isRegularFile()
                  .hasParent(fileContext.path)
                  .hasFileName(filename)
              }
            }
            context("and a directory exists at the path") {
              val fileNameString = "pre-existing-directory"
              modifyFixture {
                fileContext.path.newDirectory(fileNameString)
              }

              test("then a FileAlreadyExistsException is thrown by explicitly passing in the action") {
                expectThrows<FileAlreadyExistsException> {
                  fileContext.run {
                    fileNameString(action) {}
                  }
                }
              }
              test("then a FileAlreadyExistsException is thrown using default action") {
                expectThrows<FileAlreadyExistsException> {
                  fileContext.run {
                    fileNameString {}
                  }
                }
              }
            }
            context("and file already exists, and the provided content is $Original") {
              val originalContent = "this is the original file content"
              test("then the file is retrieved with its original content by explicitly passing in the action") {
                val filename = it.name
                fileContext.path.newFile(filename, originalContent.toByteArray())
                val context = fileContext.run {
                  filename(action, content = Original)
                }
                assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(fileContext.path)
              }
              test("using default parameter action") {
                val filename = it.name
                fileContext.path.newFile(filename, originalContent.toByteArray())
                val context = fileContext.run {
                  filename(content = Original)
                }
                assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(fileContext.path)
              }
              test("using default parameter action and using default content parameter of $Original") {
                val filename = it.name
                fileContext.path.newFile(filename, originalContent.toByteArray())
                val context = fileContext.run {
                  filename {
                  }
                }
                assertThat(context).isInstanceOf(FileContext.RegularFileContext::class.java)
                assertThat(context.path)
                  .hasContent(originalContent)
                  .hasFileName(filename)
                  .hasParent(fileContext.path)
              }
            }

            context("with text content but a directory exists at path then FileAlreadyExistsException is thrown") {
              before { fileContext.path.newDirectory(it.name) }
              val content = "this is file content"
              test("by explicitly passing in the action") {
                val filename = it.name
                fileContext.path.newDirectory(filename)
                expectThrows<FileAlreadyExistsException> {
                  fileContext.run {
                    filename(action, content = content)
                  }
                }
              }
              test("using default action") {
                val filename = it.name
                fileContext.path.newDirectory(filename)
                expectThrows<FileAlreadyExistsException> {
                  fileContext.run {
                    filename(content = content)
                  }
                }
              }
            }
          }
        }
        derivedContext<FileActionFixture>("is Create") {
          deriveFixture { FileActionFixture(parentFixture, FileAction.Create) }
          context("when a file is requested") {
            test("and the file does not exist then it is created") {
              val fileContext: FileContext.RegularFileContext = fileContext.file("non-existent-file", action)
              assertThat(fileContext.path)
                .isRegularFile()
                .hasParent(this.fileContext.path)
            }
            test("and the file exists then a FileAlreadyExistsException is thrown") {
              fileContext.path.newFile("preExistingFile")
              expectThrows<FileAlreadyExistsException> {
                fileContext.file("preExistingFile",
                  action)
              }
            }
          }

          context("when a nested file is requested") {
            test("and the parent directory doesn't exist then a NoSuchFileException is thrown") {
              expectThrows<NoSuchFileException> {
                fileContext.file("path/to/nonExistentDir/file",
                  action)
              }
            }
            test("and the the parent directory exists then the file is created") {
              val dirPath = fileContext.path.newDirectory("path/to/dir")
              val context: FileContext.RegularFileContext = fileContext.file("path/to/dir/newFile", action)
              assertThat(context.path)
                .hasParent(dirPath)
                .isRegularFile()
                .endsWith(Paths.get("newFile"))
            }
            test("and the file already exists then a FileAlreadyExistsException is thrown") {
              val file = fileContext.path.newDirectory("path/to/dir").newFile("preExistingFile")
              expectThrows<FileAlreadyExistsException> {
                fileContext.file("path/to/dir/${file.fileNameString}",
                  action)
              }
            }

            test("and a directory already exists then a FileAlreadyExistsException is thrown") {
              fileContext.path.newDirectory("path/to/dir")
              expectThrows<FileAlreadyExistsException> {
                fileContext.file("path/to/dir",
                  action)
              }
            }
          }

          context("when a directory is requested") {
            test("and the directory doesn't exist then then it is created") {
              assertThat(fileContext.directory("nonExistentDir", action).path)
                .isDirectory()
                .hasParent(fileContext.path)
                .hasFileName("nonExistentDir")
            }
            test("and the directory exists then a FileAlreadyExistsException is thrown") {
              fileContext.path.newDirectory("preExistingDirectory")
              expectThrows<FileAlreadyExistsException> {
                fileContext.directory("preExistingDirectory",
                  action)
              }
            }
            test("and file already exists then a FileAlreadyExistsException is thrown") {
              fileContext.path.newFile("preExistingFile")
              expectThrows<FileAlreadyExistsException> {
                fileContext.directory("preExistingFile",
                  action)
              }
            }
          }

          context("when a nested directory is requested") {
            test("and the nested directory doesn't exist then the nested path is created") {
              assertThat(fileContext.directory("nested/dir/path", action).path)
                .isDirectory()
                .startsWith(fileContext.path)
                .endsWith(Paths.get("nested/dir/path"))
            }
            test("and a portion of the path exists then the rest of the path is created") {
              val nestedDir = fileContext.path.newDirectory("nested/dir")
              assertThat(fileContext.directory("nested/dir/farther/nesting", action).path)
                .isDirectory()
                .startsWith(fileContext.path)
                .startsWith(nestedDir)
                .endsWith(Paths.get("farther", "nesting"))
            }
            test("and the nested directory exists then a FileAlreadyExistsException thrown") {
              fileContext.path.newDirectory("pre/existing/nested/dir")
              expectThrows<FileAlreadyExistsException> {
                fileContext.directory("pre/existing/nested/dir",
                  action)
              }
            }
          }
        }

        derivedContext<FileActionFixture>("is Get") {
          deriveFixture { FileActionFixture(parentFixture, FileAction.Get) }

          test("when a file does not exist then a NoSuchFileException is thrown") {
            expectThrows<NoSuchFileException> { fileContext.file("nonExistentFile", action) }
          }

          test("when a file is requested and the file exists then the context is retrieved") {
            val file = fileContext.path.newFile("preExistingFile")
            val fileContext: FileContext.RegularFileContext = fileContext.file(file.fileNameString, action)
            assertThat(fileContext.path)
              .hasParent(this.fileContext.path)
              .isRegularFile()
              .isEqualTo(file)
          }

          test("when a nested file is requested and the file exists then the context is retrieved") {
            val file = fileContext.path.newDirectory("path/to/dir").newFile("preExistingFile")
            val fileContext: FileContext.RegularFileContext = fileContext.file("path/to/dir/preExistingFile",
              action)
            assertThat(fileContext.path)
              .isRegularFile()
              .startsWith(this.fileContext.path)
              .endsWith(file)
          }

          test("when a file is requested and the file is a directory then a NoSuchFileException is thrown") {
            fileContext.path.newDirectory("preExistingDir")
            expectThrows<NoSuchFileException> { fileContext.file("preExistingDir", action) }
          }

          test("when a directory is requested and the directory does not exist then a NoSuchFileException is thrown") {
            expectThrows<NoSuchFileException> {
              fileContext.directory("nonExistentDirectory",
                action)
            }
          }

          test("when directory is requested and the directory exists then the context is retrieved") {
            val nestedDirectory = fileContext.path.newDirectory("preExistingDirectory")
            val context: FileContext.DirectoryContext = fileContext.directory("preExistingDirectory", action)
            assertThat(context.path)
              .isDirectory()
              .isEqualTo(nestedDirectory)
          }

          test("when nested directory is requested and the directory exists then the context is retrieved") {
            val nestedDirectory = fileContext.path.newDirectory("nested/pre/existing/dir")
            val context: FileContext.DirectoryContext = fileContext.directory("nested/pre/existing/dir", action)
            assertThat(context.path)
              .isDirectory()
              .isEqualTo(nestedDirectory)
          }

          test("when directory is requested and file exists then a NoSuchFileException is thrown") {
            fileContext.path.newFile("preExistingFile")
            expectThrows<NoSuchFileException> {
              fileContext.directory("preExistingFile",
                action)
            }
          }
        }
        context("when div invocation is used") {
          test("and directory does not exist then it is created") {
            val subdirContext: FileContext.DirectoryContext = fixture / "nonExistentSubDirectory"
            assertThat(subdirContext.path)
              .isDirectory()
              .hasFileName("nonExistentSubDirectory")
              .hasParent(fixture.path)
          }

          test("and directory does exist then it is retrieved") {
            val subDirectory = fixture.path.newDirectory("preExistingSubDirectory")
            val subdirContext: FileContext.DirectoryContext = fixture / subDirectory.fileNameString
            assertThat(subdirContext.path)
              .isDirectory()
              .endsWith(subDirectory)
              .hasParent(fixture.path)
          }

          test("and file exists at the location then a FileAlreadyExistsException is thrown") {
            val file = fixture.path.newFile("preExistingFile")
            expectThrows<FileAlreadyExistsException> {
              fixture / file.fileNameString
            }
          }

          test("with multiple paths then the entire directory path is created") {
            val childContext: FileContext.DirectoryContext = fixture / "first" / "second" / "third"

            assertThat(childContext.path)
              .isDirectory()
              .startsWith(fixture.path)
              .endsWith(Paths.get("first", "second", "third"))
          }

          test("with lambda then the lambda is called with the same instance as the receiver") {
            var invoked = false
            val result: FileContext.DirectoryContext = fixture / {
              invoked = true
              assertThat(this)
                .isSameAs(fixture)
            }
            assertThat(invoked)
              .describedAs("Lambda body was invoked")
              .isTrue()
            assertThat(result)
              .describedAs("Operator invocation returns same instance that is receiver of div div operation")
              .isSameAs(fixture)
          }

          test("with path and lambda then the lambda is applied to the receiver that is at the path") {
            val parentPathName = "child-dir"
            var invoked = false
            var thisFromLambdaBodyOfDivInvoke: FileContext.DirectoryContext? = null
            val result: FileContext.DirectoryContext = fixture / parentPathName / {
              invoked = true
              assertThat(this).isInstanceOf(FileContext.DirectoryContext::class.java)
              assertThat(path)
                .hasFileName(parentPathName)
                .isDirectory()
                .hasParent(this@test.fixture.path) // TODO: understand why we need to quality the `fixture` with `this`s
              thisFromLambdaBodyOfDivInvoke = this
            }
            assertThat(invoked)
              .describedAs("Lambda body was invoked")
              .isTrue()
            assertThat(result)
              .describedAs("Operator invocation returns same instance that is receiver of div operation")
              .isSameAs(thisFromLambdaBodyOfDivInvoke)
          }
        }
      }
    }
  }

  @TestFactory
  internal fun `file request`() = testFactory<Unit> {
    val posixFilePermissions =
      PosixFilePermissions.asFileAttribute(
        setOf(PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.GROUP_READ)
      )

    test("Get type is an object") {
      assertThat(FileAction.Get::class.objectInstance)
        .withFailMessage("${FileAction.Get::class} must be an object instance")
        .isNotNull
    }

    test("MaybeCreate is a constant") {
      assertThat(FileAction.MaybeCreate)
        .isInstanceOf(FileAction.MaybeCreate::class.java)
    }

    test("MaybeCreate instance with attributes") {
      val request = FileAction.MaybeCreate(listOf(posixFilePermissions))
      assertThat(request.fileAttributes)
        .containsExactly(posixFilePermissions)
    }

    test("Create is a constant") {
      assertThat(FileAction.Create)
        .isInstanceOf(FileAction.Create::class.java)
    }

    test("Create instance with attributes") {
      val request = FileAction.Create(listOf(posixFilePermissions))
      assertThat(request.fileAttributes)
        .containsExactly(posixFilePermissions)
    }
  }
}
