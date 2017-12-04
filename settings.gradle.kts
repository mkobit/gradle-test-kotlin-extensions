import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

pluginManagement {
  val gradleDir = Paths.get(rootDir.path, "gradle")
  val kotlinVersion = Files.readAllLines(gradleDir.resolve("kotlin-version.txt"), StandardCharsets.UTF_8).joinToString().trim()
  val junitPlatformVersion = Files.readAllLines(gradleDir.resolve("junit-platform-version.txt"), StandardCharsets.UTF_8).joinToString().trim()
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
        useVersion(kotlinVersion)
      } else if (requested.id.id.startsWith("org.junit.platform.gradle.plugin")) {
        useModule("org.junit.platform:junit-platform-gradle-plugin:$junitPlatformVersion")
      } else if (requested.id.id.startsWith("org.jetbrains.dokka")) {
        useModule("org.jetbrains.dokka:dokka-gradle-plugin:${requested.version}")
      }
    }
  }
  repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
  }
}

rootProject.name = "gradle-test-kotlin-extensions"
