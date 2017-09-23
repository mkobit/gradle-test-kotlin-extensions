import java.io.ByteArrayOutputStream
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.platform.console.options.Details
import org.junit.platform.gradle.plugin.JUnitPlatformExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

buildscript {
  repositories {
    mavenCentral()
    jcenter()
  }
  dependencies {
    // TODO: load from properties or script plugin
    classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.15")
  }
}

plugins {
  `java-library`
  `maven-publish`
  kotlin("jvm")
  id("com.github.ben-manes.versions") version "0.15.0"
}

val gitCommitSha: String by lazy {
  ByteArrayOutputStream().use {
    project.exec {
      commandLine("git", "rev-parse", "HEAD")
      standardOutput = it
    }
    it.toString(Charsets.UTF_8.name()).trim()
  }
}

tasks.withType(Jar::class.java) {
  manifest {
    attributes(mapOf(
      "Build-Revision" to gitCommitSha,
      "Implementation-Version" to project.version
      // TODO: include Gradle version?
    ))
  }
}

val SourceSet.kotlin: SourceDirectorySet
  get() = (this as HasConvention).convention.getPlugin(KotlinSourceSet::class.java).kotlin

tasks {
  "wrapper"(Wrapper::class) {
    gradleVersion = "4.2"
  }
}

version = "0.1.0"
group = "com.mkobit.gradle.test"
repositories {
  jcenter()
  mavenCentral()
}

apply {
  from("gradle/junit5.gradle.kts")
  plugin("org.junit.platform.gradle.plugin")
  plugin("org.jetbrains.dokka")
}

val kotlinVersion by project
val junitPlatformVersion: String by rootProject.extra
val junitTestImplementationArtifacts: Map<String, Map<String, String>> by rootProject.extra
val junitTestRuntimeOnlyArtifacts: Map<String, Map<String, String>> by rootProject.extra

dependencies {
  api(gradleApi())
  api(gradleTestKit())
  api(kotlin("stdlib-jre8", kotlinVersion as String))
  testImplementation(kotlin("reflect", kotlinVersion as String))
  testImplementation("org.assertj:assertj-core:3.8.0")
  testImplementation("org.mockito:mockito-core:2.10.0")
  testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
  junitTestImplementationArtifacts.values.forEach {
    testImplementation(it)
  }
  junitTestRuntimeOnlyArtifacts.values.forEach {
    testRuntimeOnly(it)
  }
  testImplementation(kotlin("stdlib-jre8", kotlinVersion as String))
}

extensions.getByType(JUnitPlatformExtension::class.java).apply {
  platformVersion = junitPlatformVersion
  filters {
    engines {
      include("junit-jupiter")
    }
  }
  logManager = "org.apache.logging.log4j.jul.LogManager"
  details = Details.TREE
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val main = java.sourceSets["main"]!!
// No Java in main source set
main.java.setSrcDirs(emptyList<Any>())

val sourcesJar by tasks.creating(Jar::class) {
  classifier = "sources"
  from(main.allSource)
  description = "Assembles a JAR of the source code"
  group = JavaBasePlugin.DOCUMENTATION_GROUP
}

val dokka by tasks.getting(DokkaTask::class) {
  dependsOn(main.classesTaskName)
  outputFormat = "html"
  outputDirectory = "$buildDir/javadoc"
  sourceDirs = main.kotlin.srcDirs
}

val javadocJar by tasks.creating(Jar::class) {
  dependsOn(dokka)
  from(dokka.outputDirectory)
  classifier = "javadoc"
  description = "Assembles a JAR of the generated Javadoc"
  group = JavaBasePlugin.DOCUMENTATION_GROUP
}

tasks["assemble"].dependsOn(sourcesJar, javadocJar)

publishing {
  publications.invoke {
    "library"(MavenPublication::class) {
      from(components["java"])
      artifact(sourcesJar)
      artifact(javadocJar)
    }
  }
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions.jvmTarget = "1.8"
}
