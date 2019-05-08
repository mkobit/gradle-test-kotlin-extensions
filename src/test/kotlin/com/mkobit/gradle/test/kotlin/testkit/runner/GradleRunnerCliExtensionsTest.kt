package com.mkobit.gradle.test.kotlin.testkit.runner

import dev.minutest.ContextBuilder
import dev.minutest.TestContextBuilder
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import testsupport.minutest.testFactory
import testsupport.stdlib.removeFirstSequnce
import kotlin.reflect.KMutableProperty1
import java.nio.file.Paths.get as Path

internal class GradleRunnerCliExtensionsTest {

  companion object {
    private val absentFromArguments: List<List<String>> = listOf(
      emptyList(),
      listOf("--other-arg"),
      listOf("--other-arg", "otherArgValue"),
      listOf("--other-arg", "otherArgValue", "--after-arg"),
      listOf("--other-arg", "otherArgValue", "--after-arg", "afterArgValue")
    )
  }

  @DisplayName("CLI")
  @TestFactory
  internal fun `CLI options and extension properties`() = testFactory<GradleRunner> {
    fixture { GradleRunner.create() }

    context("repeatable value option") {
      repeatableOptionWithValuesTestsFor(
        "--include-build",
        GradleRunner::includedBuilds,
        Path("../other-project"),
        Path("../../my-project")
      )
      repeatableOptionWithValuesTestsFor(
        "--init-script",
        GradleRunner::initScripts,
        Path("first.gradle"),
        Path("other.gradle")
      )
      repeatableOptionWithValuesTestsFor("--exclude-task", GradleRunner::excludedTasks, "taskA", "taskB")
    }
    context("single value option") {
      optionWithValueTestsFor(
        "--build-file",
        GradleRunner::buildFile,
        Path("first", "first.gradle"),
        Path("second", "second.gradle")
      )
      optionWithValueTestsFor(
        "--max-workers", GradleRunner::maxWorkers, 1, 2)
      optionWithValueTestsFor(
        "--project-cache-dir",
        GradleRunner::projectCacheDir,
        Path(".gradle-dir"),
        Path("../.gradle-project-dir")
      )
      optionWithValueTestsFor(
        "--settings-file",
        GradleRunner::settingsFile,
        Path("settings.gradle"),
        Path("settings.gradle.kts")
      )
    }
    context("boolean toggle option") {
      booleanFlagTestsFor("--build-cache", GradleRunner::buildCache)
      booleanFlagTestsFor("--configure-on-demand", GradleRunner::configureOnDemand)
      booleanFlagTestsFor("--continue", GradleRunner::continueAfterFailure)
      booleanFlagTestsFor("--debug", GradleRunner::debug)
      booleanFlagTestsFor("--dry-run", GradleRunner::dryRun)
      booleanFlagTestsFor("--info", GradleRunner::info)
      booleanFlagTestsFor("--full-stacktrace", GradleRunner::fullStacktrace)
      booleanFlagTestsFor("--no-build-cache", GradleRunner::noBuildCache)
      booleanFlagTestsFor("--no-scan", GradleRunner::noBuildScan)
      booleanFlagTestsFor("--no-parallel", GradleRunner::noParallel)
      booleanFlagTestsFor("--parallel", GradleRunner::parallel)
      booleanFlagTestsFor("--offline", GradleRunner::offline)
      booleanFlagTestsFor("--profile", GradleRunner::profile)
      booleanFlagTestsFor("--quiet", GradleRunner::quiet)
      booleanFlagTestsFor("--refresh-dependencies", GradleRunner::refreshDependencies)
      booleanFlagTestsFor("--rerun-tasks", GradleRunner::rerunTasks)
      booleanFlagTestsFor("--scan", GradleRunner::buildScan)
      booleanFlagTestsFor("--stacktrace", GradleRunner::stacktrace)
      booleanFlagTestsFor("--warn", GradleRunner::warn)
    }
  }

  /**
   * Creates multiple tests for use with a boolean toggleable option.
   * The option should be in the form of `--build-cache`, where there is no value for the option.
   * @param flag the command line flag
   * @param property a reference to the extension property
   */
  private fun ContextBuilder<GradleRunner>.booleanFlagTestsFor(
    flag: String,
    property: KMutableProperty1<GradleRunner, Boolean>
  ) {

    val presentBeforeArguments = listOf(
      listOf(flag),
      listOf(flag, "--other-arg"),
      listOf(flag, "--other-arg", "otherArgValue"),
      listOf("--other-arg", flag),
      listOf("--other-arg", "otherArgValue", flag),
      listOf("--other-arg", "otherArgValue", flag, "--after-arg"),
      listOf("--other-arg", "otherArgValue", flag, "--after-arg", "afterArgValue")
    )

    fun GradleRunner.assertProperty() = assertThat(property.get(this))
    fun GradleRunner.assertPropertyFalse() = assertProperty().isFalse()
    fun GradleRunner.assertPropertyTrue() = assertProperty().isTrue()

    context("'$flag' (property '${property.name}')") {
      fixture { GradleRunner.create() }
      context("when the argument list is") {
        absentFromArguments.forEach { args ->
          context(args.toString()) {
            modifyFixture { withArguments(args) }
            test("then the property is false") {
              assertPropertyFalse()
              assertArguments().doesNotContain(flag)
            }
            context("and the property is set to false") {
              before { property.set(this, false) }
              test("then the property is false") {
                assertPropertyFalse()
              }
              test("then the argument list does not change") {
                assertArguments()
                  .doesNotContain(flag)
                  .containsOnlyElementsOf(args)
              }
            }
            context("and the property is set to true") {
              before { property.set(this, true) }
              test("then the argument list contains the flag") {
                assertArguments().containsOnlyOnce(flag)
              }
              test("then the argument list contains all original elements") {
                assertArguments().containsAll(args)
              }
              test("then the arguments list size is the original + 1") {
                assertArguments().hasSize(args.size + 1)
              }
              test("then the property is true") {
                assertPropertyTrue()
              }
            }
          }
        }

        presentBeforeArguments.forEach { args ->
          context(args.toString()) {
            modifyFixture { withArguments(args) }
            test("then the property is true") {
              assertPropertyTrue()
            }
            context("and the property is disabled") {
              before { property.set(this, false) }
              test("then the argument list does not contain the flag") {
                assertArguments().doesNotContain(flag)
              }
              test("then the argument list contains the original arguments minus the flag") {
                assertArguments().containsOnlyElementsOf(args - flag)
              }
              test("then the property is false") {
                assertPropertyFalse()
              }
            }
            context("and the property is enabled") {
              before { property.set(this, true) }
              test("then the argument list retains all original elements") {
                assertArguments()
                  .containsOnlyOnce(flag)
                  .containsOnlyElementsOf(args)
                  .hasSize(args.size)
              }
              test("then the property is true") {
                assertPropertyTrue()
              }
            }
          }
        }
      }
    }
  }

  // TODO: generative testing for the 'Map' use case instead of copy and paste
  @Nested
  inner class RepeatedKeyValueCliOptionsTest {

    @TestFactory
    internal fun `system properties`() = testFactory<GradleRunner> {
      fixture { GradleRunner.create() }

      context("empty arguments") {
        test("getting system properties are empty") {
          assertThat(systemProperties)
            .isEmpty()
        }
        context("setting system properties") {
          context("to single map entry with String/String key/value pair") {
            modifyFixture { systemProperties = mapOf("Prop1" to "val1") }
            test("then system property is present in arguments list") {
              assertThat(arguments)
                .containsExactly("--system-prop", "Prop1=val1")
            }
            test("then system property is retrieved with getter") {
              assertThat(systemProperties)
                .isEqualTo(mapOf("Prop1" to "val1"))
            }
          }
          context("to single map entry with String/null key/value pair") {
            val stringToNull = mapOf("Prop2" to null)
            modifyFixture { systemProperties = stringToNull }
            test("then system property key is present in arguments list") {
              assertThat(arguments)
                .containsExactly("--system-prop", "Prop2")
            }
            test("then system property is retrieved with getter") {
              assertThat(systemProperties)
                .isEqualTo(stringToNull)
            }
          }
        }
        context("to multiple map entries") {
          context("where both entries have values") {
            modifyFixture { systemProperties = mapOf("Prop1" to "val1", "Prop2" to "2") }
            test("then both entries system properties are present in arguments list") {
              assertThat(arguments)
                .containsSequence("--system-prop", "Prop1=val1")
                .containsSequence("--system-prop", "Prop2=2")
            }
            test("then both values are retrieved with getter") {
              assertThat(systemProperties)
                .isEqualTo(mapOf("Prop1" to "val1", "Prop2" to "2"))
            }
          }
          context("where one entry has null value") {
            modifyFixture { systemProperties = mapOf("Prop1" to null, "Prop2" to "2") }
            test("then both entries values are present in arguments list") {
              assertThat(arguments)
                .containsSequence("--system-prop", "Prop1")
                .containsSequence("--system-prop", "Prop2=2")
            }
            test("then both values are retrieved with getter") {
              assertThat(systemProperties)
                .isEqualTo(mapOf("Prop1" to null, "Prop2" to "2"))
            }
          }
        }
      }

      context("arguments contain") {
        context("a boolean option") {
          modifyFixture { withArguments("--some-boolean-option") }
          context("and a single option and value") {
            modifyFixture { withArguments(arguments + listOf("--system-prop", "Prop1=val1")) }
            context("then settings the value to empty") {
              modifyFixture { systemProperties = emptyMap() }
              test("removes the options from arguments list") {
                assertThat(arguments)
                  .doesNotContain("--system-prop", "Prop1=val1")
              }
              test("makes the property return an empty map") {
                assertThat(systemProperties)
                  .isEmpty()
              }
              test("leaves the boolean option in the arguments list") {
                assertThat(arguments)
                  .containsExactly("--some-boolean-option")
              }
            }
          }
        }

        context("another key/value option") {
          modifyFixture { withArguments("--some-key-value-option", "some-value=thing") }
          context("and a single option and value") {
            modifyFixture { withArguments(arguments + listOf("--system-prop", "Prop1=val1")) }
            context("then settings the property to empty") {
              modifyFixture { systemProperties = emptyMap() }
              test("removes the option and values from arguments list") {
                assertThat(arguments)
                  .doesNotContain("--system-prop", "Prop1=val1")
              }
              test("makes the property return an empty map") {
                assertThat(systemProperties)
                  .isEmpty()
              }
              test("leaves the boolean option in the arguments list") {
                assertThat(arguments)
                  .containsExactly("--some-key-value-option", "some-value=thing")
              }
            }
          }
        }

        context("a system property key and value") {
          modifyFixture { withArguments(listOf("--system-prop", "Prop1=val1")) }
          test("then the getter returns the system properties") {
            assertThat(systemProperties)
              .isEqualTo(mapOf("Prop1" to "val1"))
          }
        }
      }
    }

    @TestFactory
    internal fun `project properties `() = testFactory<GradleRunner> {
      fixture { GradleRunner.create() }

      context("empty arguments") {
        test("getting system properties are empty") {
          assertThat(projectProperties)
            .isEmpty()
        }
        context("setting system properties") {
          context("to single map entry with String/String key/value pair") {
            modifyFixture { projectProperties = mapOf("Prop1" to "val1") }
            test("then system property is present in arguments list") {
              assertThat(arguments)
                .containsExactly("--project-prop", "Prop1=val1")
            }
            test("then system property is retrieved with getter") {
              assertThat(projectProperties)
                .isEqualTo(mapOf("Prop1" to "val1"))
            }
          }
          context("to single map entry with String/null key/value pair") {
            val stringToNull = mapOf("Prop2" to null)
            modifyFixture { projectProperties = stringToNull }
            test("then system property key is present in arguments list") {
              assertThat(arguments)
                .containsExactly("--project-prop", "Prop2")
            }
            test("then system property is retrieved with getter") {
              assertThat(projectProperties)
                .isEqualTo(stringToNull)
            }
          }
        }
        context("to multiple map entries") {
          context("where both entries have values") {
            modifyFixture {
              projectProperties = mapOf(
                "Prop1" to "val1",
                "Prop2" to "2"
              )
            }
            test("then both entries system properties are present in arguments list") {
              assertThat(arguments)
                .containsSequence("--project-prop", "Prop1=val1")
                .containsSequence("--project-prop", "Prop2=2")
            }
            test("then both system properties are retrieved with getter") {
              assertThat(projectProperties)
                .isEqualTo(
                  mapOf(
                    "Prop1" to "val1",
                    "Prop2" to "2"
                  )
                )
            }
          }
          context("where one entry has null value") {
            modifyFixture {
              projectProperties = mapOf(
                "Prop1" to null,
                "Prop2" to "2"
              )
            }
            test("then both entries system properties are present in arguments list") {
              assertThat(arguments)
                .containsSequence("--project-prop", "Prop1")
                .containsSequence("--project-prop", "Prop2=2")
            }
            test("then both system properties are retrieved with getter") {
              assertThat(projectProperties)
                .isEqualTo(
                  mapOf(
                    "Prop1" to null,
                    "Prop2" to "2"
                  )
                )
            }
          }
        }
      }

      context("arguments contain") {
        context("a boolean option") {
          modifyFixture { withArguments("--some-boolean-option") }
          context("and a system property key and value") {
            modifyFixture { withArguments(arguments + listOf("--project-prop", "Prop1=val1")) }
            context("then settings the system properties to empty") {
              modifyFixture { projectProperties = emptyMap() }
              test("removes the system properties from arguments list") {
                assertThat(arguments)
                  .doesNotContain("--project-prop", "Prop1=val1")
              }
              test("makes the system properties getter return an empty map") {
                assertThat(projectProperties)
                  .isEmpty()
              }
              test("leaves the boolean option in the arguments list") {
                assertThat(arguments)
                  .containsExactly("--some-boolean-option")
              }
            }
          }
        }

        context("another key/value option") {
          modifyFixture { withArguments("--some-key-value-option", "some-value=thing") }
          context("and a system property key and value") {
            modifyFixture { withArguments(arguments + listOf("--project-prop", "Prop1=val1")) }
            context("then settings the system properties to empty") {
              modifyFixture { projectProperties = emptyMap() }
              test("removes the system properties from arguments list") {
                assertThat(arguments)
                  .doesNotContain("--project-prop", "Prop1=val1")
              }
              test("makes the system properties getter return an empty map") {
                assertThat(projectProperties)
                  .isEmpty()
              }
              test("leaves the boolean option in the arguments list") {
                assertThat(arguments)
                  .containsExactly("--some-key-value-option", "some-value=thing")
              }
            }
          }
        }

        context("a system property key and value") {
          modifyFixture { withArguments(listOf("--project-prop", "Prop1=val1")) }
          test("then the getter returns the system properties") {
            assertThat(projectProperties)
              .isEqualTo(mapOf("Prop1" to "val1"))
          }
        }
      }
    }
  }

  private fun <T : Any> ContextBuilder<GradleRunner>.repeatableOptionWithValuesTestsFor(
    option: String,
    property: KMutableProperty1<GradleRunner, List<T>>,
    firstValue: T,
    secondValue: T
  ) {
    require(firstValue != secondValue) { "values for testing must be different" }

    fun GradleRunner.assertProperty() = assertThat(property.get(this))

    /**
     * Tests for setting the property to a single value.
     */
    fun TestContextBuilder<GradleRunner, GradleRunner>.setToSingleValueTests(singleValue: T) {
      class SingleValueFixture(
        val runner: GradleRunner,
        val previousArguments: List<String>
      )
      derivedContext<SingleValueFixture>("and the property is set to a single value") {
        deriveFixture {
          SingleValueFixture(parentFixture, arguments)
        }

        before { property.set(runner, listOf(singleValue)) }

        test("then getting the property value is equal to that value") {
          runner.assertProperty().containsOnly(singleValue)
        }
        test("then the arguments contains the option and value") {
          runner.assertArguments().containsSequence(option, firstValue.toString())
        }
        test("then the arguments contain all original arguments") {
          runner.assertArguments().containsAll(previousArguments) // TODO: consider making containsSequence
        }
        test("then the arguments size is the original + 2") {
          runner.assertArguments().hasSize(previousArguments.size + 2)
        }
      }
    }

    context("'$option' (property '${property.name}')") {
      fixture { GradleRunner.create() }
      context("when the argument list is") {
        absentFromArguments.forEach { args ->
          context(args.toString()) {
            modifyFixture { withArguments(args) }
            test("then getting the extension property value is empty") {
              assertProperty().isEmpty()
            }
            context("and the property is set to an empty collection") {
              before { property.set(fixture, emptyList()) }
              test("then the arguments list does not change") {
                assertArguments().containsExactlyElementsOf(args)
              }
              test("then the property is still empty") {
                assertProperty().isEmpty()
              }
            }
            setToSingleValueTests(firstValue)
            context("and the property is set to multiple values") {
              before { property.set(fixture, listOf(firstValue, secondValue)) }
              test("then getting the property value is equal to the values") {
                assertProperty().containsOnly(firstValue, secondValue)
              }
              context("then the arguments contain") {
                test("the original arguments") {
                  assertArguments().containsAll(args) // TODO: consider making containsSequence
                }
                test("the option and values") {
                  assertArguments()
                    .containsSequence(option, firstValue.toString())
                    .containsSequence(option, secondValue.toString())
                }
                test("then the arguments size is the original + 4") {
                  assertArguments().hasSize(args.size + 4)
                }
              }
            }
          }
        }

        listOf(
          listOf(option, firstValue),
          listOf(option, firstValue, "--other-arg"),
          listOf(option, firstValue, "--other-arg", "otherArgValue"),
          listOf("--other-arg", option, firstValue),
          listOf("--other-arg", "otherArgValue", option, firstValue),
          listOf("--other-arg", "otherArgVdalue", option, firstValue, "--after-arg"),
          listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg", "afterArgValue")
        ).map { it.map { arg -> arg.toString() } }
          .forEach { argsContainingOptionAndValue ->
            context(argsContainingOptionAndValue.toString()) {
              modifyFixture { withArguments(argsContainingOptionAndValue) }
              test("then property is the option value") {
                assertProperty().containsExactly(firstValue)
              }
              context("when property set to empty") {
                before { property.set(fixture, emptyList()) }
                test("then property is empty") {
                  assertProperty().isEmpty()
                }
                context("then the arguments") {
                  test("contain the other original arguments") {
                    assertArguments()
                      .containsOnlyElementsOf(argsContainingOptionAndValue - listOf(option, firstValue.toString()))
                  }
                  test("has size of the original - 2") {
                    assertArguments().hasSize(argsContainingOptionAndValue.size - 2)
                  }
                  test("does not contain the option") {
                    assertArguments().doesNotContain(option)
                  }
                  test("does not contain the option value") {
                    assertArguments().doesNotContain(firstValue.toString())
                  }
                }
              }
              context("when property set to two values") {
                before { property.set(fixture, listOf(secondValue, firstValue)) }
                test("then property is empty") {
                  assertProperty().containsExactly(secondValue, firstValue)
                  context("then the arguments") {
                    test("contain the other original arguments") {
                      assertArguments()
                        .containsOnlyElementsOf(argsContainingOptionAndValue)
                    }
                    test("has size of the original + 2") {
                      assertArguments().hasSize(argsContainingOptionAndValue.size + 2)
                    }
                    test("contains both options and values") {
                      assertArguments()
                        .containsSequence(option, secondValue.toString())
                        .containsSequence(option, firstValue.toString())
                    }
                  }
                }
              }
            }
          }
        listOf(
          listOf(option, firstValue, option, secondValue),
          listOf(option, firstValue, option, secondValue, "--other-arg"),
          listOf(option, firstValue, option, secondValue, "--other-arg", "otherArgValue"),
          listOf("--other-arg", option, firstValue, option, secondValue),
          listOf("--other-arg", "otherArgValue", option, firstValue, option, secondValue),
          listOf("--other-arg", "otherArgValue", option, firstValue, option, secondValue, "--after-arg"),
          listOf("--other-arg",
            "otherArgValue",
            option,
            firstValue,
            option,
            secondValue,
            "--after-arg",
            "afterArgValue"),
          listOf(option,
            firstValue,
            "--other-arg",
            "otherArgValue",
            "--after-arg",
            "afterArgValue",
            option,
            secondValue)
        ).map { it.map { arg -> arg.toString() } }
          .forEach { argsContainingBothOptionsAndValues ->
            context(argsContainingBothOptionsAndValues.toString()) {
              modifyFixture { withArguments(argsContainingBothOptionsAndValues) }
              test("then the property contains both values") {
                assertProperty().containsExactly(firstValue, secondValue)
              }
              context("when property set to empty") {
                before { property.set(fixture, emptyList()) }
                test("then property is empty") {
                  assertProperty().isEmpty()
                }
                context("then the arguments") {
                  test("contain the other original arguments") {
                    assertArguments()
                      .containsOnlyElementsOf(
                        argsContainingBothOptionsAndValues
                          .removeFirstSequnce(listOf(option, firstValue.toString()))
                          .removeFirstSequnce(listOf(option, secondValue.toString()))
                      )
                  }
                  test("has size of the original - 4") {
                    assertArguments().hasSize(argsContainingBothOptionsAndValues.size - 4)
                  }
                  test("does not contain the option") {
                    assertArguments().doesNotContain(option)
                  }
                  test("does not contain the option values") {
                    assertArguments().doesNotContain(firstValue.toString(), secondValue.toString())
                  }
                }
              }
              context("when property set to a single value") {
                before { property.set(fixture, listOf(firstValue)) }
                test("then property is the single value") {
                  assertProperty().containsExactly(firstValue)
                }
                context("then the arguments") {
                  test("contain the other original arguments") {
                    assertArguments()
                      .containsOnlyElementsOf(
                        argsContainingBothOptionsAndValues.removeFirstSequnce(
                          listOf(option, secondValue.toString())
                        )
                      )
                  }
                  test("has size of the original - 2") {
                    assertArguments().hasSize(argsContainingBothOptionsAndValues.size - 2)
                  }
                  test("contains the specified option and value") {
                    assertArguments().containsSequence(option, firstValue.toString())
                  }
                  test("does not contain the removed option and value") {
                    assertArguments().doesNotContainSequence(option, secondValue.toString())
                  }
                  test("does not contain the removed value") {
                    assertArguments().doesNotContain(secondValue.toString())
                  }
                }
              }
            }
          }
      }
    }
  }

  private fun <T : Any> ContextBuilder<GradleRunner>.optionWithValueTestsFor(
    option: String,
    property: KMutableProperty1<GradleRunner, T?>,
    firstValue: T,
    secondValue: T
  ) {
    require(firstValue != secondValue) { "values for testing must be different" }

    val presentBeforeArguments: List<List<String>> = listOf(
      listOf(option, firstValue),
      listOf(option, firstValue, "--other-arg"),
      listOf(option, firstValue, "--other-arg", "otherArgValue"),
      listOf("--other-arg", option, firstValue),
      listOf("--other-arg", "otherArgValue", option, firstValue),
      listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg"),
      listOf("--other-arg", "otherArgValue", option, firstValue, "--after-arg", "afterArgValue")
    ).map { it.map { arg -> arg.toString() } }

    fun GradleRunner.assertProperty() = assertThat(property.get(this))

    context("'$option' (property '${property.name}')") {
      fixture { GradleRunner.create() }
      context("when the argument list is") {
        absentFromArguments.forEach { args ->
          context(args.toString()) {
            modifyFixture { withArguments(args) }
            test("then the property is null") {
              assertProperty().isNull()
            }
            context("and the property is set to null") {
              before { property.set(this, null) }
              test("then the property is false") {
                assertProperty().isNull()
              }
              test("then the argument list does not change") {
                assertArguments().containsOnlyElementsOf(args)
              }
            }
            context("and the property is set to a value") {
              before { property.set(this, firstValue) }
              test("then the argument list contains the option and value") {
                assertArguments().containsSequence(option, firstValue.toString())
              }

              test("then the property value is equal to what was set") {
                assertProperty().isEqualTo(firstValue)
              }
              test("then the argument list contains all original elements") {
                assertArguments().containsAll(args)
              }
              test("then the arguments list size is the original + 2") {
                assertArguments().hasSize(args.size + 2)
              }
            }
          }
        }

        presentBeforeArguments.forEach { args ->
          context(args.toString()) {
            modifyFixture { withArguments(args) }
            test("then the property is equal to the present value") {
              assertProperty().isEqualTo(firstValue)
            }
            context("and the property is set to null") {
              before { property.set(this, null) }
              test("then the argument list contains the original arguments minus the option and value") {
                assertArguments().containsOnlyElementsOf(args - listOf(option, firstValue.toString()))
              }
              test("then the property is null") {
                assertProperty().isNull()
              }
            }
            context("and the property is set to a different value") {
              before { property.set(this, secondValue) }
            }
            context("and the property is set to the same value") {
              before { property.set(this, firstValue) }
              test("then the property is that value") {
                assertProperty().isEqualTo(firstValue)
              }
              test("then the argument list does not change") {
                assertArguments().containsOnlyElementsOf(args)
              }
              test("then the option and value are present") {
                assertArguments().containsSequence(option, firstValue.toString())
              }
            }
          }
        }
      }
    }
  }

  private fun GradleRunner.assertArguments() = assertThat(arguments)
}
