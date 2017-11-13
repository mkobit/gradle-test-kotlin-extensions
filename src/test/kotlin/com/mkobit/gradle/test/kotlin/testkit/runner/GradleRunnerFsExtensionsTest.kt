package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class GradleRunnerFsExtensionsTest {

  private lateinit var runner: GradleRunner

  @BeforeEach
  internal fun setUp() {
    runner = GradleRunner.create()
  }

  @Test
  internal fun `projectDirPath extension`() {
    assertThat(runner.projectDir)
        .isNull()
    assertThat(runner.projectDirPath)
        .isNull()

    val file = File("/tmp")
    runner.withProjectDir(file)
    assertThat(runner.projectDirPath)
        .isEqualTo(file.toPath())
  }
}
