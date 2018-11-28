package com.mkobit.gradle.test.kotlin.examples

import com.mkobit.gradle.test.kotlin.io.FileAction
import com.mkobit.gradle.test.kotlin.io.Original
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.projectDirPath
import com.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

@ExtendWith(TempDirectory::class)
internal class FileSystemExample {

  @Test
  internal fun `file system manipulation`(@TempDirectory.TempDir directory: Path) {
    val gradleRunner = GradleRunner.create().apply {
      projectDirPath = directory
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
                when(text) {
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

    assertThat(buildResult.projectDir.resolve("build").resolve("synced"))
        .isDirectory()
        .satisfies { synced ->
          assertThat(synced.resolve("file1.txt"))
              .isRegularFile()
              .hasContent("some text in here").satisfies { file1 ->
                  assertThat(Files.getPosixFilePermissions(file1))
                    .doesNotContain(PosixFilePermission.GROUP_READ)
              }
          assertThat(synced.resolve("file2.txt"))
              .isRegularFile()
              .hasContent("""
                some text content
                some text content with specified encoding
                some byte array content
              """.trimIndent())
          assertThat(synced.resolve("dir1"))
              .isDirectory()
              .satisfies { dir1 ->
                assertThat(dir1.resolve("file1.txt"))
                    .hasContent("""
                      changed content
                      additional content
                    """.trimIndent())
                assertThat(dir1.resolve("dir2"))
                    .isDirectory()
                    .satisfies { dir2 ->
                      assertThat(dir2.resolve("file1.txt"))
                          .isRegularFile()
                          .hasContent("dir2 content")
                      assertThat(dir2.resolve("dir3"))
                          .isDirectory().satisfies { dir3 ->
                          assertThat(dir3.resolve("file1.txt"))
                                .isRegularFile()
                                .hasContent("nested dir content")
                          }
                    }
              }
        }
  }
}
