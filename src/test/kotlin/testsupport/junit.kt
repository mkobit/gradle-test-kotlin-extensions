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
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path


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

/**
 * Borrowed from JUnit 5 - https://raw.githubusercontent.com/junit-team/junit5/master/platform-tests/src/test/java/org/junit/jupiter/extensions/TempDirectory.java
 */
class TempDirectory : AfterEachCallback, ParameterResolver {

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.VALUE_PARAMETER)
  annotation class Root

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
      parameterContext.parameter.isAnnotationPresent(Root::class.java) && parameterContext.parameter.type == Path::class.java

  override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): Any {
    return getLocalStore(context).getOrComputeIfAbsent<String, Any>(KEY) { key -> createTempDirectory(context) }
  }

  @Throws(Exception::class)
  override fun afterEach(context: ExtensionContext) {
    val tempDirectory = getLocalStore(context).get(KEY, Path::class.java)
    if (tempDirectory != null) {
      delete(tempDirectory)
    }
  }

  private fun getLocalStore(context: ExtensionContext): ExtensionContext.Store {
    return context.getStore(localNamespace(context))
  }

  private fun localNamespace(context: ExtensionContext): ExtensionContext.Namespace =
      ExtensionContext.Namespace.create(TempDirectory::class.java, context)

  private fun createTempDirectory(context: ExtensionContext): Path {
    try {
      val tempDirName: String
      if (context.testMethod.isPresent) {
        tempDirName = context.testMethod.get().name
      } else if (context.testClass.isPresent) {
        tempDirName = context.testClass.get().name
      } else {
        tempDirName = context.displayName
      }

      return Files.createTempDirectory(tempDirName)
    } catch (e: IOException) {
      throw ParameterResolutionException("Could not create temp directory", e)
    }

  }

  @Throws(IOException::class)
  private fun delete(tempDirectory: Path) {
    Files.walkFileTree(tempDirectory, object : SimpleFileVisitor<Path>() {

      @Throws(IOException::class)
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        return deleteAndContinue(file)
      }

      @Throws(IOException::class)
      override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        return deleteAndContinue(dir)
      }

      @Throws(IOException::class)
      private fun deleteAndContinue(path: Path): FileVisitResult {
        Files.delete(path)
        return FileVisitResult.CONTINUE
      }
    })
  }

  companion object {

    private const val KEY = "tempDirectory"
  }
}
