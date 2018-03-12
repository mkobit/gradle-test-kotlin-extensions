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

/**
 * Borrowed from JUnit 5 - https://raw.githubusercontent.com/junit-team/junit5/master/platform-tests/src/test/java/org/junit/jupiter/extensions/TempDirectory.java
 */
class TempDirectory : AfterEachCallback, ParameterResolver {

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.VALUE_PARAMETER)
  annotation class Root

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.VALUE_PARAMETER)
  annotation class File(val parentPath: String = "")

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.VALUE_PARAMETER)
  annotation class Directory(val parentPath: String = "")

  companion object {
    private val KEY = TempDirectory::class
  }

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    return parameterContext.parameter.run {
      AnnotationSupport.isAnnotated(this, Root::class.java) ||
          AnnotationSupport.isAnnotated(this, File::class.java) ||
          AnnotationSupport.isAnnotated(this, Directory::class.java)
    } && parameterContext.parameter.type == Path::class.java
  }

  override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): Path {
    val root = getLocalStore(context).getOrComputeIfAbsent<KClass<TempDirectory>, Path>(
        KEY,
        { _ -> createTempDirectory(context) },
        Path::class.java
    )
    return parameterContext.parameter.run {
      when {
        AnnotationSupport.isAnnotated(this, Root::class.java) -> root
        AnnotationSupport.isAnnotated(this, File::class.java) -> {
          val file = AnnotationSupport.findAnnotation(this, File::class.java).get()
          if (file.parentPath.isBlank()) {
            val p = root.resolve(UUID.randomUUID().toString())
            Files.createFile(p)
          } else {
            val directory = Files.createDirectories(root.resolve(file.parentPath))
            Files.createFile(directory.resolve(UUID.randomUUID().toString()))
          }
        }
        AnnotationSupport.isAnnotated(this, Directory::class.java) -> {
          val directory = AnnotationSupport.findAnnotation(this, Directory::class.java).get()
          if (directory.parentPath.isBlank()) {
          Files.createDirectory(root.resolve(UUID.randomUUID().toString()))
          } else {
            Files.createDirectories(root.resolve(directory.parentPath).resolve(UUID.randomUUID().toString()))
          }
        }
        else -> throw ParameterResolutionException("could not resolve parameter $this")
      }
    }
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
      val tempDirName: String = when {
        context.testMethod.isPresent -> context.testMethod.get().name
        context.testClass.isPresent -> context.testClass.get().name
        else -> context.displayName
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
}
