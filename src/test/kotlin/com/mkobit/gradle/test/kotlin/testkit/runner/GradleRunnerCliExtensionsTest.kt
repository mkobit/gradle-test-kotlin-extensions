package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.ReflectionSupport
import testsupport.dynamicGradleRunnerTest
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GradleRunnerCliExtensionsTest {

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
      BooleanFlag(flag = "--offline", stateApply = OfflineEnabled::class),
      BooleanFlag(flag = "--profile", stateApply = ProfileEnabled::class)
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

  @TestFactory
  internal fun `system properties`(): Stream<DynamicNode> {
    val stringToString1 = mapOf("Prop1" to "val1")
    val stringToNull = mapOf("Prop2" to null)
    val stringToString2 = mapOf("Prop3" to "3")

    return Stream.of(
        dynamicGradleRunnerTest("empty arguments") {
          assertThat(systemProperties)
              .isEmpty()
        },
        dynamicGradleRunnerTest("reset arguments to empty") {
          systemProperties = mapOf("SomeKey" to "SomeValue")
          assertThat(systemProperties)
              .hasSize(1)
          systemProperties = emptyMap()
          assertThat(systemProperties)
              .isEmpty()
        },
        dynamicGradleRunnerTest("single map entry with String/String key/value pair") {
            systemProperties = stringToString1
            assertThat(systemProperties)
                .isEqualTo(stringToString1)
            assertThat(arguments)
                .containsExactly("--system-prop", "Prop1=val1")
          },
        dynamicGradleRunnerTest("single map entry with String/null key/value pair") {
          systemProperties = stringToNull
          assertThat(systemProperties)
              .isEqualTo(stringToNull)
          assertThat(arguments)
              .containsExactly("--system-prop", "Prop2")
        },
        dynamicGradleRunnerTest("multiple map entries") {
          systemProperties = stringToString1 + stringToString2
          assertThat(systemProperties)
              .isEqualTo(stringToString1 + stringToString2)
          assertThat(arguments)
              .containsSequence("--system-prop", "Prop1=val1")
              .containsSequence("--system-prop", "Prop3=3")
        },
        dynamicGradleRunnerTest("multiple map entries including a String/null entry") {
          systemProperties = stringToString1 + stringToNull
          assertThat(systemProperties)
              .isEqualTo(stringToString1 + stringToNull)
          assertThat(arguments)
              .containsSequence("--system-prop", "Prop1=val1")
              .containsSequence("--system-prop", "Prop2")
        }
    )
  }

  @TestFactory
  internal fun `project properties `(): Stream<DynamicNode> {
    val stringToString1 = mapOf("Prop1" to "val1")
    val stringToNull = mapOf("Prop2" to null)
    val stringToString2 = mapOf("Prop3" to "3")

    return Stream.of(
        dynamicGradleRunnerTest("empty arguments") {
          assertThat(projectProperties)
              .isEmpty()
        },
        dynamicGradleRunnerTest("reset arguments to empty") {
          projectProperties = mapOf("SomeKey" to "SomeValue")
          assertThat(projectProperties)
              .hasSize(1)
          projectProperties = emptyMap()
          assertThat(projectProperties)
              .isEmpty()
        },
        dynamicGradleRunnerTest("single map entry with String/String key/value pair") {
          projectProperties = stringToString1
          assertThat(projectProperties)
              .isEqualTo(stringToString1)
          assertThat(arguments)
              .containsExactly("--project-prop", "Prop1=val1")
        },
        dynamicGradleRunnerTest("single map entry with String/null key/value pair") {
          projectProperties = stringToNull
          assertThat(projectProperties)
              .isEqualTo(stringToNull)
          assertThat(arguments)
              .containsExactly("--project-prop", "Prop2")
        },
        dynamicGradleRunnerTest("multiple map entries") {
          projectProperties = stringToString1 + stringToString2
          assertThat(projectProperties)
              .isEqualTo(stringToString1 + stringToString2)
          assertThat(arguments)
              .containsSequence("--project-prop", "Prop1=val1")
              .containsSequence("--project-prop", "Prop3=3")
        },
        dynamicGradleRunnerTest("multiple map entries including a String/null entry") {
          projectProperties = stringToString1 + stringToNull
          assertThat(projectProperties)
              .isEqualTo(stringToString1 + stringToNull)
          assertThat(arguments)
              .containsSequence("--project-prop", "Prop1=val1")
              .containsSequence("--project-prop", "Prop2")
        }
    )
  }

  @TestFactory
  internal fun `init scripts`(): Stream<DynamicNode> {
    return Stream.of(
        dynamicGradleRunnerTest("empty arguments") {
          assertThat(initScripts)
              .isEmpty()
        },
        dynamicGradleRunnerTest("reset arguments to empty") {
          initScripts = listOf("init-script.gradle")
          assertThat(initScripts)
              .hasSize(1)
          initScripts = emptyList()
          assertThat(initScripts)
              .isEmpty()
        },
        dynamicGradleRunnerTest("single init script") {
          initScripts = listOf("init-script.gradle")
          assertThat(initScripts)
              .isEqualTo(listOf("init-script.gradle"))
          assertThat(arguments)
              .containsExactly("--init-script", "init-script.gradle")
        },
        dynamicGradleRunnerTest("multiple init scripts") {
          initScripts = listOf("init-script.gradle", "other-script.gradle")
          assertThat(initScripts)
              .isEqualTo(listOf("init-script.gradle", "other-script.gradle"))
          assertThat(arguments)
              .containsSequence("--init-script", "init-script.gradle")
              .containsSequence("--init-script", "other-script.gradle")
        }
    )
  }

  @TestFactory
  internal fun `exclude tasks`(): Stream<DynamicNode> {
    return Stream.of(
        dynamicGradleRunnerTest("empty arguments") {
          assertThat(excludedTasks)
              .isEmpty()
        },
        dynamicGradleRunnerTest("reset arguments to empty") {
          excludedTasks = listOf("taskA")
          assertThat(excludedTasks)
              .hasSize(1)
          excludedTasks = emptyList()
          assertThat(excludedTasks)
              .isEmpty()
        },
        dynamicGradleRunnerTest("single init script") {
          excludedTasks = listOf("taskA")
          assertThat(excludedTasks)
              .isEqualTo(listOf("taskA"))
          assertThat(arguments)
              .containsExactly("--exclude-task", "taskA")
        },
        dynamicGradleRunnerTest("multiple init scripts") {
          excludedTasks = listOf("taskA", "taskB")
          assertThat(excludedTasks)
              .isEqualTo(listOf("taskA", "taskB"))
          assertThat(arguments)
              .containsSequence("--exclude-task", "taskA")
              .containsSequence("--exclude-task", "taskB")
        }
    )
  }

  @TestFactory
  internal fun `can specify tasks with toggleable extension arguments`(): Stream<DynamicNode> {
    return Stream.of(
        dynamicGradleRunnerTest("can add tasks before setting some toggle options") {
          arguments("check", "build")
          info = true
          stacktrace = true
          assertThat(arguments)
              .hasSize(4)
              .containsSequence("check", "build")

        },
        dynamicGradleRunnerTest("can add tasks after setting some toggle options") {
          info = true
          stacktrace = true
          arguments("check", "build")
          assertThat(arguments)
              .hasSize(4)
              .containsSequence("check", "build")

        },
        dynamicGradleRunnerTest("can add tasks with options before setting some toggle options") {
          arguments("check", "--option", "build", "--option1", "optionValue")
          info = true
          stacktrace = true
          assertThat(arguments)
              .hasSize(7)
              .containsSequence("check", "--option", "build", "--option1", "optionValue")

        },
        dynamicGradleRunnerTest("can add tasks with options after setting some toggle options") {
          info = true
          stacktrace = true
          arguments("check", "--option", "build", "--option1", "optionValue")
          assertThat(arguments)
              .hasSize(7)
              .containsSequence("check", "--option", "build", "--option1", "optionValue")

        },
        dynamicGradleRunnerTest("can add tasks before setting a key/value option") {
          arguments("check", "build")
          projectProperties = mapOf("key1" to "prop1")
          systemProperties = mapOf("key2" to "sys1")
          assertThat(arguments)
              .hasSize(6)
              .containsSequence("check", "build")
        },
        dynamicGradleRunnerTest("can add tasks after setting a key/value option") {
          projectProperties = mapOf("key1" to "prop1")
          systemProperties = mapOf("key2" to "sys1")
          arguments("check", "build")
          assertThat(arguments)
              .hasSize(6)
              .containsSequence("check", "build")
        }
    )
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

private class ProfileEnabled : BooleanFlagApply {
  override fun retrieveState(gradleRunner: GradleRunner): Boolean = gradleRunner.profile
  override fun applyChange(gradleRunner: GradleRunner, newState: Boolean) { gradleRunner.profile = newState }
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
