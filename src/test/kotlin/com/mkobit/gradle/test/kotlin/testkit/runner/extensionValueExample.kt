package com.mkobit.gradle.test.kotlin.testkit.runner

import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

val kotlin.Int.theAnswer
  get() = 42

val BuildTask.success: Boolean
  get() = outcome == TaskOutcome.SUCCESS

fun main(args: Array<String>) {
  val i = 5
  println(i::theAnswer)
  println(Int::theAnswer)
  println(BuildTask::success)
  val b = BuildTask::success
  println(b)
}
