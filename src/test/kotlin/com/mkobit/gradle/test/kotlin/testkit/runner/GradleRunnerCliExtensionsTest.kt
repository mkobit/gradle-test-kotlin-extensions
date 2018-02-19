package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import testsupport.dynamicGradleRunnerTest
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.reflect.KMutableProperty1

internal class GradleRunnerCliExtensionsTest {

  companion object {
    private val absentFromArguments: List<RunnerArguments> = listOf(
        RunnerArguments("empty", emptyList()),
        RunnerArguments("single argument", listOf("--other-arg")),
        RunnerArguments("argument with value", listOf("--other-arg", "otherArgValue")),
        RunnerArguments("argument with value and a boolean option",
            listOf("--other-arg", "otherArgValue", "--after-arg")),
        RunnerArguments("multiple arguments each with value",
            listOf("--other-arg", "otherArgValue", "--after-arg", "afterArgValue"))
    )
  }

  @DisplayName("CLI")
  @TestFactory
  internal fun `CLI options and extension properties`(): Stream<DynamicNode> = Stream.of(
      dynamicContainer("repeatable value option",
          repeatableOptionWithValuesTestsFor("--exclude-task", GradleRunner::excludedTasks, "taskA", "taskB"),
          repeatableOptionWithValuesTestsFor("--init-script", GradleRunner::initScripts, "first.gradle", "other.gradle"),
          repeatableOptionWithValuesTestsFor("--include-build", GradleRunner::includedBuilds, Paths.get("../other-project"), Paths.get("../../my-project"))
      ),
      dynamicContainer("single value option",
          optionWithValueTestsFor("--build-file", GradleRunner::buildFile, Paths.get("first", "first.gradle"), Paths.get("second", "second.gradle")),
          optionWithValueTestsFor("--settings-file", GradleRunner::settingsFile, Paths.get("settings.gradle"), Paths.get("settings.gradle.kts"))
      ),
      dynamicContainer("boolean toggle option",
          booleanFlagTestsFor("--build-cache", GradleRunner::buildCacheEnabled),
          booleanFlagTestsFor("--configure-on-demand", GradleRunner::configureOnDemand),
          booleanFlagTestsFor("--continue", GradleRunner::continueAfterFailure),
          booleanFlagTestsFor("--debug", GradleRunner::debug),
          booleanFlagTestsFor("--dry-run", GradleRunner::dryRun),
          booleanFlagTestsFor("--info", GradleRunner::info),
          booleanFlagTestsFor("--full-stacktrace", GradleRunner::fullStacktrace),
          booleanFlagTestsFor("--no-build-cache", GradleRunner::buildCacheDisabled),
          booleanFlagTestsFor("--no-scan", GradleRunner::buildScanDisabled),
          booleanFlagTestsFor("--offline", GradleRunner::offline),
          booleanFlagTestsFor("--profile", GradleRunner::profile),
          booleanFlagTestsFor("--quiet", GradleRunner::quiet),
          booleanFlagTestsFor("--scan", GradleRunner::buildScanEnabled),
          booleanFlagTestsFor("--stacktrace", GradleRunner::stacktrace),
          booleanFlagTestsFor("--warn", GradleRunner::warn)
      )
  )

  /**
   * Creates multiple tests for use with a boolean toggleable option.
   * The option should be in the form of `--build-cache`, where there is no value for the option.
   * @param flag the command line flag
   * @param property a reference to the extension property
   */
  private fun booleanFlagTestsFor(
      flag: String,
      property: KMutableProperty1<GradleRunner, Boolean>
  ): DynamicNode {

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

    fun GradleRunner.assertProperty() = assertThat(property.get(this))
    fun GradleRunner.assertPropertyFalse() = assertProperty().isFalse()
    fun GradleRunner.assertPropertyTrue() = assertProperty().isTrue()

    return dynamicContainer("$flag mapped to property ${property.name}", listOf(
        dynamicContainer("when the flag is absent in an argument list that is",
            absentFromArguments.flatMap { (description, args) ->
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

  // TODO: generative testing for the 'Map' use case
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

  // TODO: generative testing for the 'Map' use case
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

  private fun <T : Any> repeatableOptionWithValuesTestsFor(
      option: String,
      property: KMutableProperty1<GradleRunner, List<T>>,
      firstValue: T,
      secondValue: T
  ): DynamicNode {
    require(firstValue != secondValue) { "values for testing must be different" }

    val firstValueInArguments = listOf(
        RunnerArguments("only a single option/value", listOf(option, firstValue)),
        RunnerArguments("single option/value and boolean option", listOf(option, firstValue, "--other-arg")),
        RunnerArguments("single option/value and argument with value",
            listOf(option, firstValue, "--other-arg", "otherArgValue")),
        RunnerArguments("boolean option and single option/value", listOf("--other-arg", option, firstValue)),
        RunnerArguments("option with value and single option/value",
            listOf("--other-arg", "otherArgValue", option, firstValue)),
        RunnerArguments("single option/value in middle of option with value and boolean option",
            listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg")),
        RunnerArguments("single option/value in middle of multiple options with values",
            listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg", "afterArgValue"))
    )

    val bothValuesInArguments = listOf(
        RunnerArguments("only the multiple options/values",
            listOf(option, firstValue, option, secondValue)),
        RunnerArguments("multiple options/values and boolean option",
            listOf(option, firstValue, option, secondValue, "--other-arg")),
        RunnerArguments("specified option/value and argument with value",
            listOf(option, firstValue, option, secondValue, "--other-arg", "otherArgValue")),
        RunnerArguments("boolean option and multiple options/values",
            listOf("--other-arg", option, firstValue, option, secondValue)),
        RunnerArguments("option with value and multiple options/values",
            listOf("--other-arg", "otherArgValue", option, firstValue, option, secondValue)),
        RunnerArguments("multiple options/values in middle of option with value and boolean option",
            listOf("--other-arg",
                "otherArgValue",
                option,
                firstValue,
                option,
                secondValue,
                "--after-arg")),
        RunnerArguments("multiple options/values in middle of multiple options with values",
            listOf("--other-arg",
                "otherArgValue",
                option,
                firstValue,
                option,
                secondValue,
                "--after-arg",
                "afterArgValue")),
        RunnerArguments("multiple options/values at both ends with multiple options separating",
            listOf(option,
                firstValue,
                "--other-arg",
                "otherArgValue",
                "--after-arg",
                "afterArgValue",
                option,
                secondValue))
    )

    fun GradleRunner.assertProperty() = assertThat(property.get(this))

    return dynamicContainer("$option mapped to property ${property.name}", listOf(
        dynamicContainer("when it is absent in an argument list that is",
            absentFromArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property value is empty") {
                    withArguments(args)
                    assertProperty().isEmpty()
                    assertArguments().containsExactlyElementsOf(args)
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                  },
                  dynamicGradleRunnerTest("$description and the property is set to an empty collection then the argument list does not change") {
                    withArguments(args)
                    property.set(this, emptyList())
                    assertProperty().isEmpty()
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                    assertArguments().containsOnlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is set to a single value then the argument list contains that option/value, the original argument list is included, and the property value is equal to the single value") {
                    withArguments(args)
                    property.set(this, listOf(firstValue))
                    assertProperty().containsOnly(firstValue)
                    assertArguments()
                        .containsSequence(option, firstValue.toString())
                        .containsAll(args)
                        .hasSize(args.size + 2)
                  },
                  dynamicGradleRunnerTest("$description and the property is set to multiple values then the argument list contains an option/value for each, the original argument list is included, and the property value is equal to those values") {
                    withArguments(args)
                    property.set(this, listOf(firstValue, secondValue))
                    assertProperty().containsOnly(firstValue, secondValue)
                    assertArguments()
                        .containsSequence(option, firstValue.toString())
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args)
                        .hasSize(args.size + 4)
                  }
              )
            }),
        dynamicContainer("when a single value is present in an argument list that is",
            firstValueInArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property value contains only that option value") {
                    withArguments(args)
                    assertProperty().containsOnly(firstValue)
                    assertArguments().containsExactlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned an empty collection then the option is no longer present and the property is empty") {
                    withArguments(args)
                    property.set(this, emptyList())
                    assertProperty().isEmpty()
                    assertArguments()
                        .doesNotContainSequence(option, firstValue.toString())
                        .hasSize(args.size - 2)
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned a collection containing only the same value the arguments list does not change") {
                    withArguments(args)
                    property.set(this, listOf(firstValue))
                    assertProperty().containsOnly(firstValue)
                    assertArguments()
                        .containsSequence(option, firstValue.toString())
                        .containsAll(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned a collection containing only a different value then the property contains only that value and the arguments contains the new value") {
                    withArguments(args)
                    property.set(this, listOf(secondValue))
                    assertProperty().containsOnly(secondValue)
                    assertArguments()
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args - listOf(option, firstValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned multiple values then the property contains those value and the arguments contains both values") {
                    withArguments(args)
                    property.set(this, listOf(secondValue, firstValue))
                    assertProperty().containsOnly(firstValue, secondValue)
                    assertArguments()
                        .containsSequence(option, firstValue.toString())
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args)
                        .hasSize(args.size + 2)
                  }
              )
            }),
        dynamicContainer("when multiple values are present in an argument list that is",
            bothValuesInArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property value contains all values") {
                    withArguments(args)
                    assertProperty().containsOnly(firstValue, secondValue)
                    assertArguments().containsExactlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned an empty collection then the option is no longer present and the property is empty") {
                    withArguments(args)
                    property.set(this, emptyList())
                    assertProperty().isEmpty()
                    assertArguments()
                        .doesNotContainSequence(option, firstValue.toString())
                        .doesNotContainSequence(option, secondValue.toString())
                        .containsExactlyElementsOf(args - listOf(option, firstValue.toString()) - listOf(option,
                            secondValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned a collection containing only a single value then the property contains only that value and the arguments contains only that value") {
                    withArguments(args)
                    property.set(this, listOf(secondValue))
                    assertProperty().containsOnly(secondValue)
                    assertArguments()
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args - listOf(option, firstValue.toString()))
                        .hasSize(args.size - 2)
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned both original values then the argument list does not change") {
                    withArguments(args)
                    property.set(this, listOf(secondValue, firstValue))
                    assertProperty().containsExactlyInAnyOrder(firstValue, secondValue)
                    assertArguments()
                        .containsExactlyInAnyOrderElementsOf(args)
                  }
              )
            })
    ))
  }

  private fun <T : Any> optionWithValueTestsFor(
      option: String,
      property: KMutableProperty1<GradleRunner, T?>,
      firstValue: T,
      secondValue: T
  ): DynamicNode {
    require(firstValue != secondValue) { "values for testing must be different" }

    val presentBeforeArguments = listOf(
        RunnerArguments("only the specified option/value", listOf(option, firstValue)),
        RunnerArguments("specified option/value and boolean option",
            listOf(option, firstValue, "--other-arg")),
        RunnerArguments("specified option/value and argument with value",
            listOf(option, firstValue, "--other-arg", "otherArgValue")),
        RunnerArguments("boolean option and specified option/value",
            listOf("--other-arg", option, firstValue)),
        RunnerArguments("option with value and specified option/value",
            listOf("--other-arg", "otherArgValue", option, firstValue)),
        RunnerArguments("specified option/value in middle of option with value and boolean option",
            listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg")),
        RunnerArguments("specified option/value in middle of multiple options with values",
            listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg", "afterArgValue"))
    )

    fun GradleRunner.assertProperty() = assertThat(property.get(this))

    return dynamicContainer("$option mapped to property ${property.name}", listOf(
        dynamicContainer("when the option and value are absent in an argument list that is",
            absentFromArguments.flatMap { (description, args) ->
              listOf(
                  dynamicGradleRunnerTest("$description then the property value is null") {
                    withArguments(args)
                    assertProperty().isNull()
                    assertArguments().containsExactlyElementsOf(args)
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                  },
                  dynamicGradleRunnerTest("$description and the property is set to null then the argument list does not change") {
                    withArguments(args)
                    property.set(this, null)
                    assertProperty().isNull()
                    assertArguments().doesNotContain(option, firstValue.toString(), secondValue.toString())
                    assertArguments().containsOnlyElementsOf(args)
                  },
                  dynamicGradleRunnerTest("$description and the property is set then the argument list contains the option/value in addition to the argument list and the property is equal to the set value") {
                    withArguments(args)
                    property.set(this, firstValue)
                    assertProperty().isEqualTo(firstValue)
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
                    assertProperty().isEqualTo(firstValue)
                  },
                  dynamicGradleRunnerTest("$description and the property is set to null then the option/value is removed from the argument list and the property is null") {
                    withArguments(args)
                    property.set(this, null)
                    assertProperty().isNull()
                    assertArguments()
                        .doesNotContain(option, firstValue.toString(), secondValue.toString())
                        .containsOnlyElementsOf(args - listOf(option, firstValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned a different value then the argument list has the new value and the property is the new value") {
                    withArguments(args)
                    property.set(this, secondValue)
                    assertProperty().isEqualTo(secondValue)
                    assertArguments()
                        .doesNotContain(firstValue.toString())
                        .containsSequence(option, secondValue.toString())
                        .containsAll(args - listOf(option, firstValue.toString()))
                  },
                  dynamicGradleRunnerTest("$description and the property is assigned the same value then the argument list does not change") {
                    withArguments(args)
                    property.set(this, firstValue)
                    assertProperty().isEqualTo(firstValue)
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
   * Helper class to make constructing dynamic tests more straightforward.
   */
  private data class RunnerArguments constructor(val argumentDescription: String, val arguments: List<String>) {
    companion object {
      operator fun invoke(argumentDescription: String, arguments: List<Any>): RunnerArguments =
          RunnerArguments(argumentDescription, arguments.map(Any::toString))
    }
  }

  private fun GradleRunner.assertArguments() = assertThat(arguments)
}

private fun dynamicContainer(displayName: String, vararg node: DynamicNode) = dynamicContainer(displayName, node.toList())
