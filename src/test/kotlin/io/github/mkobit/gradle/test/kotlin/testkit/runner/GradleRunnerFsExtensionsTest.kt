package io.github.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.java.allLines
import strikt.java.isDirectory
import strikt.java.isRegularFile
import strikt.java.resolve
import strikt.java.text
import strikt.java.toFile
import java.nio.file.Path

internal class GradleRunnerFsExtensionsTest {

  @Test
  internal fun `cannot configure GradleRunner project directory if it is not set`() {
    expectCatching {
      GradleRunner.create().setupProjectDir { }
    }.isFailure()
      .isA<IllegalStateException>()
  }

  @Test
  internal fun `set up a Gradle project using full methods DSL`(@TempDir root: Path) {
    GradleRunner.create().withProjectDir(root.toFile()).setupProjectDir {
      file("settings.gradle") { content = "// settings.gradle".toByteArray() }
      file("build.gradle") { content = "// build.gradle".toByteArray() }
      directory("src/main/java") {
        file("MainClass.java") { content = "public class Hello {}".toByteArray() }
        directory("com/mkobit/") {
          file("NestedDude.java") {
            content = "package io.github.mkobit;${System.lineSeparator()}".toByteArray()
            append("public class NestedDude {}${System.lineSeparator()}".toByteArray())
            append("// Additional appended content${System.lineSeparator()}".toByteArray())
          }
        }
      }
    }

    expect {
      that(root) {
        resolve("settings.gradle")
          .isRegularFile()
          .toFile()
          .text()
          .isEqualTo("// settings.gradle")
        resolve("build.gradle")
          .isRegularFile()
          .toFile()
          .text()
          .isEqualTo("// build.gradle")
        resolve("src/main/java")
          .isDirectory()
        resolve("src/main/java/MainClass.java")
          .isRegularFile()
          .toFile()
          .text()
          .isEqualTo("public class Hello {}")
        resolve("src/main/java/com/mkobit")
          .isDirectory()
        resolve("src/main/java/com/mkobit/NestedDude.java")
          .isRegularFile()
          .allLines()
          .containsExactly(
            "package io.github.mkobit;",
            "public class NestedDude {}",
            "// Additional appended content"
          )
      }
    }
  }
}
