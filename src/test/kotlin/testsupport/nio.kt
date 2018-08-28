package testsupport

import java.nio.file.Files
import java.nio.file.Path

/**
 * Creates a new file with the provided [fileName] and (optional) [content].
 */
fun Path.newFile(fileName: String, content: ByteArray? = null): Path = Files.createFile(this.resolve(fileName)).also {
  if (content != null) {
    Files.write(it, content)
  }
}

/**
 * Creates a new directory with the provided [directoryName].
 */
fun Path.newDirectory(directoryName: String): Path = Files.createDirectories(this.resolve(directoryName))

/**
 * Get the [Path.getFileName] as a [String]
 */
val Path.fileNameString get() = fileName.toString()
