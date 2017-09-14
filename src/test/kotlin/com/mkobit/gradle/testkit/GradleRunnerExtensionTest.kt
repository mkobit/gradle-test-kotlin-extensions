package com.mkobit.gradle.testkit

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class GradleRunnerExtensionTest {

  @Test
  internal fun `resolve path from projectDir`() {
    val projectDirPath = Paths.get("a")
    val mockGradleRunner: GradleRunner = mock {
      on { projectDir } doReturn projectDirPath.toFile()
    }

    val resolvePath = Paths.get("b")
    val actual = mockGradleRunner.resolveFromProjectDir(resolvePath)
    assertThat(actual)
      .startsWithRaw(projectDirPath)
      .endsWithRaw(resolvePath)
  }

  @Test
  internal fun `cannot resolve path when projectDir not set`() {
    val mockGradleRunner: GradleRunner = mock()
    whenever(mockGradleRunner.projectDir).thenReturn(null)

    assertThatIllegalStateException().isThrownBy { mockGradleRunner.resolveFromProjectDir(Paths.get("a")) }
  }
}
