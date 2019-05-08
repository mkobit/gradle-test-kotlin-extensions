package com.mkobit.gradle.test.kotlin.examples

import com.mkobit.gradle.test.kotlin.io.FileAction
import com.mkobit.gradle.test.kotlin.io.Original
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import com.mkobit.gradle.test.kotlin.testkit.runner.withProjectDir
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.allLines
import strikt.assertions.containsExactly
import strikt.assertions.isDirectory
import strikt.assertions.isRegularFile
import strikt.assertions.resolve
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

internal class FileSystemExample {

  @Test
  internal fun `file system manipulation`(@TempDir directory: Path) {
    val gradleRunner = GradleRunner.create().apply {
      withProjectDir(directory)
      setupProjectDir {
        file("settings.gradle") {
          content = "rootProject.name = 'example-dsl-project'".toByteArray()
        }
        file("build.gradle") {
          content = """
            plugins {
              id 'lifecycle-base'
            }

            tasks.create('syncFiles', Sync) {
              from(file('myFiles'))
              into("${'$'}buildDir/synced")
            }
          """.trimIndent().toByteArray()
        }
        directory("myFiles") {
          file("file1.txt") {
            content = "some text in here".toByteArray()
            posixFilePermissions -= setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ)
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

    expect {
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
}
