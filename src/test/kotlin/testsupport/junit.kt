package testsupport

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicTest

/**
 * Creates a [DynamicTest.dynamicTest] with the provided [displayName] and provides a new [GradleRunner] instance
 * to the [executable] as a receiver for more idiomatic extensions parameter/method tests.
 */
fun dynamicGradleRunnerTest(
    displayName: String,
    executable: GradleRunner.() -> Unit
): DynamicTest = DynamicTest.dynamicTest(displayName) {
  GradleRunner.create().executable()
}
