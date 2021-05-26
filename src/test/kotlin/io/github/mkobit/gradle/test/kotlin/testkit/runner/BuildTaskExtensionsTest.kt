package io.github.mkobit.gradle.test.kotlin.testkit.runner

import io.mockk.every
import io.mockk.mockk
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import testsupport.minutest.testFactory

internal class BuildTaskExtensionsTest {

  @TestFactory
  internal fun `outcome extension properties`() = testFactory<Unit> {
    val allExtensionValuesToOutcome = mapOf(
      BuildTask::success to TaskOutcome.SUCCESS,
      BuildTask::upToDate to TaskOutcome.UP_TO_DATE,
      BuildTask::skipped to TaskOutcome.SKIPPED,
      BuildTask::noSource to TaskOutcome.NO_SOURCE,
      BuildTask::fromCache to TaskOutcome.FROM_CACHE,
      BuildTask::failed to TaskOutcome.FAILED
    )

    class Fixture(
      matchedOutcome: TaskOutcome
    ) {
      val buildTask: BuildTask = mockk {
        every { outcome } returns matchedOutcome
      }
    }

    allExtensionValuesToOutcome.forEach { (matchingExtensionProperty, matchedOutcome) ->
      derivedContext<Fixture>("when task outcome is $matchedOutcome") {
        fixture { Fixture(matchedOutcome) }
        test("then extension property $matchingExtensionProperty returns true") {
          expectThat(matchingExtensionProperty.get(buildTask))
            .isTrue()
        }

        // for all other property accessors
        (allExtensionValuesToOutcome.keys - matchingExtensionProperty).forEach { nonMatchingExtensionProperty ->
          test("then extension property $nonMatchingExtensionProperty returns false") {
            expectThat(nonMatchingExtensionProperty.get(buildTask))
              .isFalse()
          }
        }
      }
    }
  }
}
