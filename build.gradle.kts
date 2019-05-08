import buildsrc.ProjectInfo
import com.jfrog.bintray.gradle.BintrayExtension
import java.io.ByteArrayOutputStream
import java.net.URL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
  id("com.gradle.build-scan") version "2.2.1"
  `java-library`
  `maven-publish`
  kotlin("jvm") version "1.3.31"
  id("nebula.release") version "10.1.1"
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.jfrog.bintray") version "1.8.4"
  id("org.jetbrains.dokka") version "0.9.18"
  id("org.jlleitschuh.gradle.ktlint") version "7.4.0"
}

group = "com.mkobit.gradle.test"
description = "Kotlin library to aid in writing tests for Gradle"

val gitCommitSha: String by lazy {
  ByteArrayOutputStream().use {
    project.exec {
      commandLine("git", "rev-parse", "HEAD")
      standardOutput = it
    }
    it.toString(Charsets.UTF_8.name()).trim()
  }
}

ktlint {
  version.set("0.32.0")
}

val SourceSet.kotlin: SourceDirectorySet
  get() = withConvention(KotlinSourceSet::class) { kotlin }

buildScan {
  fun env(key: String): String? = System.getenv(key)

  termsOfServiceAgree = "yes"
  termsOfServiceUrl = "https://gradle.com/terms-of-service"

  // Env variables from https://circleci.com/docs/2.0/env-vars/
  if (env("CI") != null) {
    logger.lifecycle("Running in CI environment, setting build scan attributes.")
    tag("CI")
    env("CIRCLE_BRANCH")?.let { tag(it) }
    env("CIRCLE_BUILD_NUM")?.let { value("Circle CI Build Number", it) }
    env("CIRCLE_BUILD_URL")?.let { link("Build URL", it) }
    env("CIRCLE_SHA1")?.let { value("Revision", it) }
//    Issue with Circle CI/Gradle with caret (^) in URLs
//    see: https://discuss.gradle.org/t/build-scan-plugin-1-10-3-issue-when-using-a-url-with-a-caret/24965
//    see: https://discuss.circleci.com/t/circle-compare-url-does-not-url-escape-caret/18464
//    env("CIRCLE_COMPARE_URL")?.let { link("Diff", it) }
    env("CIRCLE_REPOSITORY_URL")?.let { value("Repository", it) }
    env("CIRCLE_PR_NUMBER")?.let { value("Pull Request Number", it) }
    link("Repository", ProjectInfo.projectUrl)
  }
}

repositories {
  jcenter()
  mavenCentral()
}

configurations.all {
  resolutionStrategy.eachDependency {
    when (requested.group) {
      "dev.minutest" -> useVersion("1.6.0")
      "org.junit.jupiter" -> useVersion("5.4.2")
      "org.junit.platform" -> useVersion("1.4.2")
      "io.strikt" -> useVersion("0.20.0")
      "org.apache.logging.log4j" -> useVersion("2.11.2")
    }
  }
}

dependencies {
  api(gradleApi())
  api(gradleTestKit())
  api(kotlin("stdlib-jdk8"))

  implementation("io.github.microutils:kotlin-logging:1.6.26")

  testImplementation(kotlin("reflect"))
  testImplementation("org.assertj:assertj-core:3.12.2")
  testImplementation("io.mockk:mockk:1.9.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("dev.minutest:minutest")
  testImplementation("io.strikt:strikt-core")
  testImplementation("io.strikt:strikt-gradle")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.apache.logging.log4j:log4j-core")
  testRuntimeOnly("org.apache.logging.log4j:log4j-jul")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val main = sourceSets["main"]!!
// No Java in main source set
main.java.setSrcDirs(emptyList<Any>())

tasks {
  wrapper {
    gradleVersion = "5.4.1"
  }

  withType<Jar>().configureEach {
    from(project.projectDir) {
      include("LICENSE.txt")
      into("META-INF")
    }
    manifest {
      attributes(mapOf(
        "Build-Revision" to gitCommitSha,
        "Automatic-Module-Name" to ProjectInfo.automaticModuleName,
        "Implementation-Version" to project.version
        // TODO: include Gradle version?
      ))
    }
  }

  withType<Javadoc>().configureEach {
    options {
      header = project.name
      encoding = "UTF-8"
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }

  test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    testLogging {
      events("skipped", "failed")
    }
  }

  (release) {
    enabled = false
  }

  jar {
    enabled = false
  }

  val sourcesJar by registering(Jar::class) {
    archiveClassifier.set("sources")
    from(main.allSource)
    description = "Assembles a JAR of the source code"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  dokka {
    dependsOn(main.classesTaskName)
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    // See https://github.com/Kotlin/dokka/issues/196
    externalDocumentationLink {
      url = URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/")
    }
    externalDocumentationLink {
      url = URL("https://docs.oracle.com/javase/8/docs/api/")
    }
  }

  val javadocJar by registering(Jar::class) {
    dependsOn(dokka)
    from(dokka.map { it.outputDirectory })
    archiveClassifier.set("javadoc")
    description = "Assembles a JAR of the generated Javadoc"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  assemble {
    dependsOn(sourcesJar, javadocJar)
  }

  (release) {
    dependsOn(bintrayUpload)
    // disabled to not push git tag
    enabled = false
  }
}

val publicationName = "gradleTestKotlinExtensions"
publishing {
  publications {
    val sourcesJar by tasks.getting
    val javadocJar by tasks.getting
    register(publicationName, MavenPublication::class) {
      from(components["java"])
      artifact(sourcesJar)
      artifact(javadocJar)
      pom {
        description.set(project.description)
        url.set(ProjectInfo.projectUrl)
        licenses {
          license {
            name.set("The MIT License")
            url.set("https://opensource.org/licenses/MIT")
            distribution.set("repo")
          }
        }
      }
    }
  }
}

bintray {
  user = System.getenv("BINTRAY_USER")
  key = System.getenv("BINTRAY_KEY")
  publish = true
  setPublications(publicationName)
  pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
    repo = "gradle"
    name = project.name
    userOrg = "mkobit"
    setLabels("gradle", "test", "plugins", "kotlin")
    setLicenses("MIT")
    desc = project.description
    websiteUrl = ProjectInfo.projectUrl
    issueTrackerUrl = ProjectInfo.issuesUrl
    vcsUrl = ProjectInfo.scmUrl
  })
}
