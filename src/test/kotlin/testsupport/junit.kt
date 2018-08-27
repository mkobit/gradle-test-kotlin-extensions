package testsupport

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.SimpleFileVisitor
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.platform.commons.support.AnnotationSupport
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.reflect.KClass

class BooleanArgumentsProvider : ArgumentsProvider {
  override fun provideArguments(context: ExtensionContext): Stream<out Arguments> = Stream.of(
      Arguments.of(true),
      Arguments.of(false)
  )
}

@ArgumentsSource(BooleanArgumentsProvider::class)
annotation class BooleanSource

fun dynamicGradleRunnerTest(
    displayName: String,
    executable: GradleRunner.() -> Unit
): DynamicTest = DynamicTest.dynamicTest(displayName) {
  GradleRunner.create().executable()
}
