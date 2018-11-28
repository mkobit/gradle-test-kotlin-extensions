package com.mkobit.gradle.test.kotlin.testkit.runner

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.reflect.KProperty1

internal class BuildTaskExtensionsTest {

  @TestFactory
  internal fun `can get outcome`(): Stream<DynamicNode> = Stream.of(
      testsFor(TaskOutcome.SUCCESS, BuildTask::success),
      testsFor(TaskOutcome.UP_TO_DATE, BuildTask::upToDate),
      testsFor(TaskOutcome.SKIPPED, BuildTask::skipped),
      testsFor(TaskOutcome.NO_SOURCE, BuildTask::noSource),
      testsFor(TaskOutcome.FROM_CACHE, BuildTask::fromCache),
      testsFor(TaskOutcome.FAILED, BuildTask::failed)
  )

  private fun testsFor(
      targetedOutcome: TaskOutcome,
      valueRetrieval: KProperty1<BuildTask, Boolean>
  ): DynamicNode {
    val otherOutcomes = TaskOutcome.values()
        .filter { it != targetedOutcome }
        .stream()
        .map { taskOutcome ->
          dynamicTest("when task outcome is $taskOutcome then property evaluates to false") {
            val buildTask: BuildTask = mock()
            whenever(buildTask.outcome).thenReturn(taskOutcome)
            assertThat(valueRetrieval.get(buildTask))
                .isFalse()
          }
        }
    val matchingOutcome = Stream.of(dynamicTest("when outcome is $targetedOutcome then property evaluates to true") {
      val buildTask: BuildTask = mock()
      whenever(buildTask.outcome).thenReturn(targetedOutcome)
      assertThat(valueRetrieval.get(buildTask))
          .isTrue()
    })

    return dynamicContainer(
        "extension property ${valueRetrieval.name}",
        Stream.concat(otherOutcomes, matchingOutcome)
    )
  }
}
