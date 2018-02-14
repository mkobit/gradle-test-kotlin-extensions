package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import testsupport.dynamicGradleRunnerTest
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.reflect.KMutableProperty1

internal class GradleRunnerCliExtensionsTest {

  @TestFactory
  internal fun `boolean property`(): Stream<DynamicNode> = Stream.of(
      booleanFlagTestsFor("--build-cache", GradleRunner::buildCacheEnabled),
      booleanFlagTestsFor("--no-build-cache", GradleRunner::buildCacheDisabled),
      booleanFlagTestsFor("--continue", GradleRunner::continueAfterFailure),
      booleanFlagTestsFor("--quiet", GradleRunner::quiet),
      booleanFlagTestsFor("--stacktrace", GradleRunner::stacktrace),
      booleanFlagTestsFor("--full-stacktrace", GradleRunner::fullStacktrace),
      booleanFlagTestsFor("--info", GradleRunner::info),
      booleanFlagTestsFor("--dry-run", GradleRunner::dryRun),
      booleanFlagTestsFor("--debug", GradleRunner::debug),
      booleanFlagTestsFor("--warn", GradleRunner::warn),
      booleanFlagTestsFor("--scan", GradleRunner::buildScanEnabled),
      booleanFlagTestsFor("--no-scan", GradleRunner::buildScanDisabled),
      booleanFlagTestsFor("--offline", GradleRunner::offline),
      booleanFlagTestsFor("--profile", GradleRunner::profile)
  )

  /**
   * Creates a bunch of dynamic tests for use with a boolean toggleable option.
   * The option should be in the form of `--build-cache`, where there is no value for the option.
   * @param flag the command line flag
   * @param property a reference to the extension property
   */
  private fun booleanFlagTestsFor(
      flag: String,
      property: KMutableProperty1<GradleRunner, Boolean>
  ): DynamicNode {

    val absentBeforeArguments = listOf(
        RunnerArguments("empty", emptyList()),
        RunnerArguments("single argument", listOf("--other-arg")),
        RunnerArguments("argument with value", listOf("--other-arg", "otherArgValue")),
        RunnerArguments("argument with value and a boolean option",
            listOf("--other-arg", "otherArgValue", "--after-arg")),
        RunnerArguments("multiple arguments each with value",
            listOf("--other-arg", "otherArgValue", "--after-arg", "afterArgValue"))
    )
    val presentBeforeArguments = listOf(
        RunnerArguments("only the flag", listOf(flag)),
        RunnerArguments("flag and boolean option", listOf(flag, "--other-arg")),
        RunnerArguments("flag and argument with value", listOf(flag, "--other-arg", "otherArgValue")),
        RunnerArguments("boolean option and flag", listOf("--other-arg", flag)),
        RunnerArguments("option with value and flag", listOf("--other-arg", "otherArgValue", flag)),
        RunnerArguments("flag in middle of option with value and boolean option",
            listOf("--other-arg", "otherArgValue", flag, "--after-arg")),
        RunnerArguments("flag in middle of multiple options with values",
            listOf("--other-arg", "otherArgValue", flag, "--after-arg", "afterArgValue"))
    )

    fun GradleRunner.assertPropertyFalse() = assertThat(property.get(this)).isFalse()
    fun GradleRunner.assertPropertyTrue() = assertThat(property.get(this)).isTrue()

    return dynamicContainer("${property.name} for flag $flag", listOf(
        dynamicContainer("when the flag is absent in an argument list that is",
            absentBeforeArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property is false") {
                    withArguments(args)
                    assertPropertyFalse()
                    assertArguments().doesNotContain(flag)
                  },
                  dynamicGradleRunnerTest("$description and the property is disabled then the argument list does not change") {
                    withArguments(args)
                    property.set(this, false)
                    assertPropertyFalse()
                    assertArguments().doesNotContain(flag)
                    assertArguments().containsOnlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is enabled then the argument list contains the flag in addition to the argument list and the property is true") {
                    withArguments(args)
                    property.set(this, true)
                    assertPropertyTrue()
                    assertArguments()
                        .containsOnlyOnce(flag)
                        .containsAll(args)
                        .hasSize(args.size + 1)
                  }
              )
            }),
        dynamicContainer("when the flag is present in an argument list that is",
            presentBeforeArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property is true") {
                    withArguments(args)
                    assertArguments().contains(flag)
                    assertPropertyTrue()
                  },
                  dynamicGradleRunnerTest("$description and the property is disabled then the argument list does not contain the flag and the property is false") {
                    withArguments(args)
                    property.set(this, false)
                    assertPropertyFalse()
                    assertArguments().doesNotContain(flag)
                    assertArguments().containsOnlyElementsOf(args - flag)
                  },
                  dynamicGradleRunnerTest("$description and the property is enabled then the argument list does not change") {
                    withArguments(args)
                    property.set(this, true)
                    assertPropertyTrue()
                    assertArguments()
                        .containsOnlyOnce(flag)
                        .containsOnlyElementsOf(args)
                        .hasSize(args.size)
                  }
              )
            })
    ))
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
  internal fun `single value options`(): Stream<DynamicNode> = Stream.of(
      optionWithValueTestsFor("--build-file", GradleRunner::buildFile, Paths.get("first", "first.gradle"), Paths.get("second", "second.gradle"))
  )

  private fun <T> optionWithValueTestsFor(
      option: String,
      property: KMutableProperty1<GradleRunner, T?>,
      firstValue: T,
      secondValue: T
  ): DynamicNode {
    require(firstValue != secondValue) { "values for testing must be different" }

    val absentBeforeArguments = listOf(
        RunnerArguments("empty", emptyList()),
        RunnerArguments("single argument", listOf("--other-arg")),
        RunnerArguments("argument with value", listOf("--other-arg", "otherArgValue")),
        RunnerArguments("argument with value and a boolean option",
            listOf("--other-arg", "otherArgValue", "--after-arg")),
        RunnerArguments("multiple arguments each with value",
            listOf("--other-arg", "otherArgValue", "--after-arg", "afterArgValue"))
    )
    val presentBeforeArguments = listOf(
        RunnerArguments("only the specified option/value", listOf(option, firstValue.toString())),
        RunnerArguments("specified option/value and boolean option", listOf(option, firstValue.toString(), "--other-arg")),
        RunnerArguments("specified option/value and argument with value", listOf(option, firstValue.toString(), "--other-arg", "otherArgValue")),
        RunnerArguments("boolean option and specified option/value", listOf("--other-arg", option, firstValue.toString())),
        RunnerArguments("option with value and specified option/value", listOf("--other-arg", "otherArgValue", option, firstValue.toString())),
        RunnerArguments("specified option/value in middle of option with value and boolean option",
            listOf("--other-arg", "otherArgValue", option, firstValue.toString(), "--after-arg")),
        RunnerArguments("specified option/value in middle of multiple options with values",
            listOf("--other-arg", "otherArgValue", option, firstValue.toString(), "--after-arg", "afterArgValue"))
    )

    fun GradleRunner.assertPropertyNull() = assertThat(property.get(this)).isNull()
    fun GradleRunner.assertPropertyEqualToFirstValue() = assertThat(property.get(this)).isEqualTo(firstValue)
    fun GradleRunner.assertPropertyEqualToSecondValue() = assertThat(property.get(this)).isEqualTo(secondValue)

    return dynamicContainer("${property.name} for flag $option", listOf(
        dynamicContainer("when the option and value are absent in an argument list that is",
            absentBeforeArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property value is null") {
                    withArguments(args)
                    assertPropertyNull()
                    assertArguments().containsExactlyElementsOf(args)
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                  },
                  dynamicGradleRunnerTest("$description and the property value is set to null then the argument list does not change") {
                    withArguments(args)
                    property.set(this, null)
                    assertPropertyNull()
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                    assertArguments().containsOnlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property value is set then the argument list contains the option/value in addition to the argument list and the property is equal to the set value") {
                    withArguments(args)
                    property.set(this, firstValue)
                    assertPropertyEqualToFirstValue()
                    assertArguments()
                        .containsSequence(option, firstValue.toString())
                        .containsAll(args)
                        .hasSize(args.size + 2)
                  }
              )
            }),
        dynamicContainer("when the option and value are present in an argument list that is",
            presentBeforeArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property is equal to the value") {
                    withArguments(args)
                    assertArguments().containsExactlyElementsOf(args)
                    assertPropertyEqualToFirstValue()
                  },
                  dynamicGradleRunnerTest("$description and the property is set to null then the option/value is removed from the argument list and the property is null") {
                    withArguments(args)
                    property.set(this, null)
                    assertPropertyNull()
                    assertArguments()
                        .doesNotContain(option, firstValue.toString(), secondValue.toString())
                        .containsOnlyElementsOf(args - listOf(option, firstValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned a different value then the argument list has the new value and the property is the new value") {
                    withArguments(args)
                    property.set(this, secondValue)
                    assertPropertyEqualToSecondValue()
                    assertArguments()
                        .doesNotContain(firstValue.toString())
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args - listOf(option, firstValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned the same value then the argument list does not change") {
                    withArguments(args)
                    property.set(this, firstValue)
                    assertPropertyEqualToFirstValue()
                    assertArguments()
                        .containsOnlyElementsOf(args)
                        .containsSequence(option, firstValue.toString())
                  }
              )
            })
    ))
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

  /**
   * Helper class to make constructing dynamic tests more straightforward.r
   */
  private data class RunnerArguments(val argumentDescription: String, val arguments: List<String>)

  private fun GradleRunner.assertArguments() = assertThat(arguments)
}
