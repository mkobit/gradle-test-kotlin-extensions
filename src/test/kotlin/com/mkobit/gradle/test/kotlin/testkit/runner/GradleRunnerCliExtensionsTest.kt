package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.ReflectionSupport
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GradleRunnerCliExtensionsTest {

  @TestTemplate
  @ExtendWith(ToggleableCliArgumentTemplateContextProvider::class)
  @BooleanFlags(
      BooleanFlag(flag = "--build-cache", stateApply = BuildCacheEnabled::class),
      BooleanFlag(flag = "--no-build-cache", stateApply = BuildCacheDisabled::class),
      BooleanFlag(flag = "--continue", stateApply = ContinueEnabled::class),
      BooleanFlag(flag = "--quiet", stateApply = QuietEnabled::class),
      BooleanFlag(flag = "--stacktrace", stateApply = StacktraceEnabled::class),
      BooleanFlag(flag = "--full-stacktrace", stateApply = FullStacktraceEnabled::class),
      BooleanFlag(flag = "--info", stateApply = InfoEnabled::class),
      BooleanFlag(flag = "--dry-run", stateApply = DryRunEnabled::class),
      BooleanFlag(flag = "--debug", stateApply = DebugEnabled::class),
      BooleanFlag(flag = "--warn", stateApply = WarnEnabled::class),
      BooleanFlag(flag = "--scan", stateApply = BuildScanEnabled::class),
      BooleanFlag(flag = "--no-scan", stateApply = BuildScanDisabled::class),
      BooleanFlag(flag = "--offline", stateApply = OfflineEnabled::class)
  )
  internal fun `boolean flag toggling in different states`(runnerContext: RunnerContext) {
    val gradleRunner = runnerContext.gradleRunner
    if (runnerContext.enabledBefore) {
      assertThat(gradleRunner.arguments)
          .contains(runnerContext.argument)
    } else {
      assertThat(gradleRunner.arguments)
          .doesNotContain(runnerContext.argument)
    }
    assertThat(runnerContext.booleanFlagApply.retrieveState(gradleRunner))
        .isEqualTo(runnerContext.enabledBefore)
    runnerContext.booleanFlagApply.applyChange(gradleRunner, runnerContext.setToState)
    assertThat(runnerContext.booleanFlagApply.retrieveState(gradleRunner))
        .isEqualTo(runnerContext.setToState)
    if (runnerContext.setToState) {
      assertThat(gradleRunner.arguments)
          .contains(runnerContext.argument)
    } else {
      assertThat(gradleRunner.arguments)
          .doesNotContain(runnerContext.argument)
    }
  }

  @Test
  internal fun `project properties`() {
    val gradleRunner = GradleRunner.create()
    assertThat(gradleRunner.projectProperties)
        .isEmpty()

    val stringToString = mapOf("Prop1" to "val1")
    val stringToNull = mapOf("Prop2" to null)
    gradleRunner.projectProperties = stringToString
    assertThat(gradleRunner.projectProperties)
        .isEqualTo(stringToString)
    assertThat(gradleRunner.arguments)
        .containsExactly("--project-prop", "Prop1=val1")

    gradleRunner.projectProperties = emptyMap()
    assertThat(gradleRunner.projectProperties)
        .isEmpty()
    assertThat(gradleRunner.arguments)
        .isEmpty()

    gradleRunner.projectProperties = stringToString  + stringToNull
    assertThat(gradleRunner.projectProperties)
        .isEqualTo(stringToString  + stringToNull)
    assertThat(gradleRunner.arguments)
        .hasSize(4)
        .containsSequence("--project-prop", "Prop1=val1")
        .containsSequence("--project-prop", "Prop2")
  }

  @Test
  internal fun `init scripts`() {
    val gradleRunner = GradleRunner.create()
    assertThat(gradleRunner.initScripts)
        .isEmpty()

    gradleRunner.initScripts = listOf("init-script.gradle")
    assertThat(gradleRunner.initScripts)
        .containsExactly("init-script.gradle")
    assertThat(gradleRunner.arguments)
        .containsExactly("--init-script", "init-script.gradle")

    gradleRunner.initScripts = emptyList()
    assertThat(gradleRunner.initScripts)
        .isEmpty()

    gradleRunner.initScripts = listOf("init-script.gradle", "other-script.gradle")
    assertThat(gradleRunner.initScripts)
        .containsExactly("init-script.gradle", "other-script.gradle")
    assertThat(gradleRunner.arguments)
        .hasSize(4)
        .containsSequence("--init-script", "init-script.gradle")
        .containsSequence("--init-script", "other-script.gradle")
  }

  @Test
  internal fun `exclude tasks`() {
    val gradleRunner = GradleRunner.create()
    assertThat(gradleRunner.excludedTasks)
        .isEmpty()

    gradleRunner.excludedTasks = listOf("aTask")
    assertThat(gradleRunner.excludedTasks)
        .containsExactly("aTask")
    assertThat(gradleRunner.arguments)
        .containsExactly("--exclude-task", "aTask")

    gradleRunner.excludedTasks = emptyList()
    assertThat(gradleRunner.initScripts)
        .isEmpty()

    gradleRunner.excludedTasks = listOf("aTask", "otherTask")
    assertThat(gradleRunner.excludedTasks)
        .containsExactly("aTask", "otherTask")
    assertThat(gradleRunner.arguments)
        .hasSize(4)
        .containsSequence("--exclude-task", "aTask")
        .containsSequence("--exclude-task", "otherTask")
  }
}

/**
 * CLI testing helper to get the state of a value and also apply a new state to it.
 */
internal interface BooleanFlagApply {
  fun applyChange(gradleRunner: GradleRunner, newState: Boolean)
  fun retrieveState(gradleRunner: GradleRunner): Boolean
}

private class BuildCacheEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.buildCacheEnabled
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.buildCacheEnabled = newState }
}

private class BuildCacheDisabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.buildCacheDisabled
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.buildCacheDisabled = newState }
}

private class ContinueEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.continueAfterFailure
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.continueAfterFailure = newState }
}

private class QuietEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.quiet
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.quiet = newState }
}

private class StacktraceEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.stacktrace
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.stacktrace = newState }
}

private class FullStacktraceEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.fullStacktrace
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.fullStacktrace = newState }
}

private class InfoEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.info
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.info = newState }
}

private class DryRunEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.dryRun
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.dryRun = newState }
}

private class DebugEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.debug
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.debug = newState }
}

private class WarnEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.warn
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.warn = newState }
}

private class BuildScanEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.buildScanEnabled
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.buildScanEnabled = newState }
}

private class BuildScanDisabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.buildScanDisabled
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.buildScanDisabled = newState }
}

private class OfflineEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.offline
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.offline = newState }
}

private class ToggleableCliArgumentTemplateContextProvider : TestTemplateInvocationContextProvider {
  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return context.testMethod
        .map {
          AnnotationSupport.isAnnotated(it, BooleanFlag::class.java) || AnnotationSupport.isAnnotated(it,
              BooleanFlags::class.java)
        }
        .orElse(false)
  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    return AnnotationSupport.findRepeatableAnnotations(context.requiredTestMethod, BooleanFlag::class.java)
        .stream()
        .map { booleanFlag: BooleanFlag ->
          booleanFlag.flag to ReflectionSupport.newInstance(booleanFlag.stateApply.java)
        }.flatMap { (booleanFlag, applyFunction) ->

      fun contextOf(
          arguments: List<String>,
          enabledBefore: Boolean, setToState: Boolean
      ): ToggleableCliArgumentInvocationContext =
          ToggleableCliArgumentInvocationContext(
              RunnerContext(
                  GradleRunner.create().withArguments(arguments),
                  booleanFlag,
                  enabledBefore,
                  setToState,
                  applyFunction
              )
          )

      val disabledBefore = Stream.of(
          emptyList(),
          listOf("--other-arg"),
          listOf("--other-arg", "otherArgValue"),
          listOf("--other-arg", "otherArgValue", "--after-arg"),
          listOf("--other-arg", "otherArgValue", "--after-arg", "afterArgValue")
      ).flatMap { arguments ->
        Stream.of(
            contextOf(arguments, false, false),
            contextOf(arguments, false, true)
        )
      }

      val enabledBefore = Stream.of(
          listOf(booleanFlag),
          listOf(booleanFlag, "--other-arg"),
          listOf(booleanFlag, "--other-arg", "otherArgValue"),
          listOf("--other-arg", booleanFlag),
          listOf("--other-arg", "otherArgValue", booleanFlag),
          listOf("--other-arg", "otherArgValue", booleanFlag, "--after-arg"),
          listOf("--other-arg", "otherArgValue", booleanFlag, "--after-arg", "afterArgValue")
      ).flatMap { arguments ->
        Stream.of(
            contextOf(arguments, true, false),
            contextOf(arguments, true, true)
        )
      }
      Stream.of(disabledBefore, enabledBefore).flatMap { it }
    }
  }
}

private class ToggleableCliArgumentInvocationContext(
    private val runnerContext: RunnerContext
) : TestTemplateInvocationContext {
  override fun getDisplayName(invocationIndex: Int): String = runnerContext.run {
    "(\"$argument\", Args=${gradleRunner.arguments}, BeforeState=$enabledBefore, ChangeTo->$setToState)"
  }

  override fun getAdditionalExtensions(): List<Extension> = listOf(RunnerContextResolver(runnerContext))
}

private class RunnerContextResolver(private val runnerContext: RunnerContext) : ParameterResolver {
  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
      parameterContext.parameter.type == RunnerContext::class.java

  override fun resolveParameter(parameterContext: ParameterContext,
                                extensionContext: ExtensionContext): Any = runnerContext
}

internal data class RunnerContext(
    val gradleRunner: GradleRunner,
    val argument: String,
    val enabledBefore: Boolean,
    val setToState: Boolean,
    val booleanFlagApply: BooleanFlagApply
)
