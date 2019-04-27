package testsupport.minutest

import dev.minutest.TestDescriptor
import java.nio.file.Files
import java.nio.file.Path
private fun TestDescriptor.joinedFullName() = fullName().joinToString(separator = "_")

private fun Path.resolveParent(testDescriptor: TestDescriptor): Path = testDescriptor.fullName()
  .dropLast(1)
  .fold(this) { accumulator: Path, descriptorName: String -> accumulator.resolve(descriptorName) }

fun Path.resolveNested(testDescriptor: TestDescriptor): Path = testDescriptor.fullName()
  .fold(this) { accumulator: Path, descriptorName: String -> accumulator.resolve(descriptorName) }

fun Path.createDirectoriesFor(testDescriptor: TestDescriptor): Path =
  Files.createDirectories(resolveNested(testDescriptor))

fun Path.createFileFor(testDescriptor: TestDescriptor): Path =
  Files.createFile(
    Files.createDirectories(resolveParent(testDescriptor))
      .resolve(testDescriptor.name)
  )
