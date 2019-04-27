package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import testsupport.assertj.assertSoftly
import testsupport.minutest.testFactory
import java.io.File
import java.nio.file.Path

internal class GradleRunnerFsExtensionsTest {

  @TestFactory
  internal fun `projectDirPath extension`() = testFactory<GradleRunner> {
    fixture { GradleRunner.create() }
    context("project directory is unset") {
      test("then projectDir extension value is null") {
        assertThat(projectDirPath)
          .isNull()
      }
    }
    context("project directory is set") {
      test("then projectDir extension value is equal to the set value") {
        val file = File("/tmp")
        withProjectDir(file)
        assertThat(projectDirPath)
          .isEqualTo(file.toPath())

      }
    }
  }

  @Test
  internal fun `cannot configure GradleRunner project directory if it is not set`() {
    assertThatThrownBy {
      GradleRunner.create().setupProjectDir {  }
    }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  internal fun `set up a Gradle project using full methods DSL`(@TempDir root: Path) {
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

    assertSoftly {
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
