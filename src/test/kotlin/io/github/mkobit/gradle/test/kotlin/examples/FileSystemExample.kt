package io.github.mkobit.gradle.test.kotlin.examples

import io.github.mkobit.gradle.test.kotlin.io.FileAction
import io.github.mkobit.gradle.test.kotlin.io.Original
import io.github.mkobit.gradle.test.kotlin.testkit.runner.KBuildResult
import io.github.mkobit.gradle.test.kotlin.testkit.runner.build
import io.github.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import io.github.mkobit.gradle.test.kotlin.testkit.runner.withProjectDir
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.java.allLines
import strikt.java.isDirectory
import strikt.java.isRegularFile
import strikt.java.resolve
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.appendBytes
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.readLines
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

internal class FileSystemExample {

  @Test
  internal fun `file system manipulation using DSL`(@TempDir directory: Path) {
    val gradleRunner = GradleRunner.create().apply {
      withProjectDir(directory)
      setupProjectDir {
        "settings.gradle.kts"(content = """rootProject.name = "example-dsl-project"""")
        "build.gradle.kts" {
          content =
            """
            plugins {
              `lifecycle-base`
            }

            tasks.register<Sync>("syncFiles") {
              from(layout.projectDirectory.dir("myFiles"))
              into(layout.buildDirectory.dir("synced"))
            }
            """.trimIndent().toByteArray()
        }
        // The div ('/') operator can be used for simple directory operations
        "myFiles" / {
          "file1.txt"(content = "some text in here")
          "file2.txt" {
            append("some text content")
            appendNewline()
            append("some text content with specified encoding", Charsets.UTF_8)
            appendNewline()
            append("some byte array content".toByteArray())
            appendNewline()
          }
          "dir1" / {
            "file1.txt" {
              content = "assign content".toByteArray()
            }
            // Behavior can be specified as parameters
            "file1.txt"(fileAction = FileAction.Get, content = Original) {
              appendNewline()
              append("additional content")
              replaceEachLine { _, text ->
                when (text) {
                  "assign content" -> "changed content"
                  else -> Original
                }
              }
            }
          }
          "dir1" / "dir2" / "dir3" / {
            "file1.txt"(content = "nested dir content")
          }
          "dir1" / "dir2" / "dir3"
          "dir1" / "dir2" / {
            "file1.txt"(content = "dir2 content")
          }
        }
      }
    }

    val buildResult = gradleRunner.build("syncFiles")
    expectFileMatchingBuildResult(buildResult)
  }

  @Test
  internal fun `file system manipulation with methods`(@TempDir directory: Path) {
    val gradleRunner = GradleRunner.create().apply {
      withProjectDir(directory)
      setupProjectDir {
        file("settings.gradle.kts") {
          content = """rootProject.name = "example-dsl-project"""".toByteArray()
        }
        file("build.gradle.kts") {
          content =
            """
            plugins {
              `lifecycle-base`
            }

            tasks.register<Sync>("syncFiles") {
              from(layout.projectDirectory.dir("myFiles"))
              into(layout.buildDirectory.dir("synced"))
            }
            """.trimIndent().toByteArray()
        }
        directory("myFiles") {
          file("file1.txt") {
            content = "some text in here".toByteArray()
            posixFilePermissions = posixFilePermissions
              .filter { it !in setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ) }
              .toSet()
          }
          file("file2.txt") {
            append("some text content")
            appendNewline()
            append("some text content with specified encoding", Charsets.UTF_8)
            appendNewline()
            append("some byte array content".toByteArray())
            appendNewline()
          }
          directory("dir1") {
            "file1.txt" {
              content = "assign content".toByteArray()
            }
            // Behavior can be specified as parameters
            file("file1.txt", fileAction = FileAction.Get) {
              appendNewline()
              append("additional content")
              replaceEachLine { _, text ->
                when (text) {
                  "assign content" -> "changed content"
                  else -> Original
                }
              }
            }
          }
          directory("dir1").directory("dir2").directory("dir3") {
            file("file1.txt") {
              content = "nested dir content".toByteArray()
            }
          }
          directory("dir1").directory("dir2") {
            file("file1.txt") {
              content = "dir2 content".toByteArray()
            }
          }
        }
      }
    }

    val buildResult = gradleRunner.build("syncFiles")

    expectFileMatchingBuildResult(buildResult)
  }

  // This example helps demonstrate why the utility methods from before are no longer needed
  @ExperimentalPathApi
  @Test
  internal fun `file system manipulation with stdlib-jdk7 method`(@TempDir directory: Path) {
    fun Path.appendNewLine() = appendText(System.lineSeparator())
    val gradleRunner = GradleRunner.create().apply {
      withProjectDir(directory)
      (directory / "settings.gradle.kts").writeText("""rootProject.name = "example-dsl-project"""")
      (directory / "build.gradle.kts").writeText(
        """
        plugins {
          `lifecycle-base`
        }

        tasks.register<Sync>("syncFiles") {
          from(layout.projectDirectory.dir("myFiles"))
          into(layout.buildDirectory.dir("synced"))
        }
        """.trimIndent()
      )
      (directory / "myFiles").createDirectories().also { myFiles ->
        (myFiles / "file1.txt").apply {
          writeText("some text in here")
          setPosixFilePermissions(
            getPosixFilePermissions()
              .filter { it !in setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ) }
              .toSet()
          )
        }
        (myFiles / "file2.txt").apply {
          createFile()
          appendText("some text content")
          appendNewLine()
          appendText("some text content with specified encoding", Charsets.UTF_8)
          appendNewLine()
          appendBytes("some byte array content".toByteArray())
          appendNewLine()
        }
        (myFiles / "dir1").createDirectories().also { dir1 ->
          (dir1 / "file1.txt").writeText("assign content")
          (dir1 / "file1.txt").apply {
            appendNewLine()
            appendText("additional content")

            writeLines(
              readLines().map { text ->
                when (text) {
                  "assign content" -> "changed content"
                  else -> text
                }
              }
            )
          }
        }
        (myFiles / "dir1" / "dir2" / "dir3").createDirectories().also { dir3 ->
          (dir3 / "file1.txt").writeText("nested dir content")
        }
        (myFiles / "dir1" / "dir2").createDirectories().also { dir2 ->
          (dir2 / "file1.txt").writeText("dir2 content")
        }
      }
    }
    val buildResult = gradleRunner.build("syncFiles")

    expectFileMatchingBuildResult(buildResult)
  }

  private fun expectFileMatchingBuildResult(buildResult: KBuildResult) = expect {
    that(buildResult)
      .get { projectDir }.and {
        resolve("build").resolve("synced").isDirectory().and {
          resolve("file1.txt")
            .isRegularFile()
            .allLines()
            .containsExactly(
              "some text in here"
            )
          resolve("file2.txt")
            .isRegularFile()
            .allLines()
            .containsExactly(
              "some text content",
              "some text content with specified encoding",
              "some byte array content"
            )

          resolve("dir1").isDirectory().and {
            resolve("file1.txt")
              .isRegularFile()
              .allLines()
              .containsExactly(
                "changed content",
                "additional content"
              )
            resolve("dir2").isDirectory().and {
              resolve("file1.txt")
                .allLines()
                .containsExactly("dir2 content")

              resolve("dir3").isDirectory().and {
                resolve("file1.txt")
                  .isRegularFile()
                  .allLines()
                  .containsExactly("nested dir content")
              }
            }
          }
        }
      }
  }
}
