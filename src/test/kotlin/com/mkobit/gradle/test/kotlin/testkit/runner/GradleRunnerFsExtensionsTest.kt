package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import testsupport.dynamicGradleRunnerTest
import java.io.File
import java.util.stream.Stream

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

  @Test
  internal fun `files created in projectDir`() {
    val runner = GradleRunner.create()
    runner.file("build.gradle") {
      "this is the file content"
    }
    runner.mkdir
  }


}
