package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

/**
 * Get the result for the task at [taskPath] or `null` if it was not included in the build.
 * @param taskPath the path of the target task
 * @return the task result or `null` if the task was not included in the build
 */
public operator fun BuildResult.get(taskPath: String): BuildTask? = task(taskPath)

/**
 * Get the result for the provided task paths in the order they are requested. If the task was not included in the build
 * then its result will be `null`.
 * @param taskPath the path of a task
 * @param taskPaths the paths of tasks
 * @return the task results in the order they were requested where a result will be `null` if it was not included in the build
 */
public operator fun BuildResult.get(taskPath: String, vararg taskPaths: String): List<BuildTask?> = listOf(task(taskPath)) + taskPaths.map(this::task)

/**
 * Get the subset of tasks that had the provided [outcome].
 * @param outcome the desired outcome
 * @return the build tasks with the given outcome
 */
public operator fun BuildResult.get(outcome: TaskOutcome): List<BuildTask> = tasks(outcome)
