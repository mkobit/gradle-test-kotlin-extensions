package testsupport

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

class BooleanArgumentsProvider : ArgumentsProvider {
  override fun provideArguments(context: ExtensionContext): Stream<out Arguments> = Stream.of(
      Arguments.of(true),
      Arguments.of(false)
  )
}

@ArgumentsSource(BooleanArgumentsProvider::class)
annotation class BooleanSource

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
