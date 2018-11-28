import buildsrc.DependencyInfo
import buildsrc.ProjectInfo
import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.Closure
import java.io.ByteArrayOutputStream
import java.net.URL
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
  id("com.gradle.build-scan") version "2.0.2"
  `java-library`
  `maven-publish`
  kotlin("jvm") version "1.3.10"
  id("com.github.ben-manes.versions") version "0.20.0"
  id("com.jfrog.bintray") version "1.8.4"
  id("org.jetbrains.dokka") version "0.9.17"
}

version = "0.6.0"
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

dependencies {
  api(gradleApi())
  api(gradleTestKit())
  implementation(DependencyInfo.kotlinLogging)
  api(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("reflect"))
  testImplementation(DependencyInfo.assertJCore)
  testImplementation(DependencyInfo.junitPioneer)
  testImplementation(DependencyInfo.mockito)
  testImplementation(DependencyInfo.mockitoKotlin)
  DependencyInfo.junitTestImplementationArtifacts.forEach {
    testImplementation(it)
  }
  DependencyInfo.junitTestRuntimeOnlyArtifacts.forEach {
    testRuntimeOnly(it)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val main = sourceSets["main"]!!
// No Java in main source set
main.java.setSrcDirs(emptyList<Any>())

tasks {
  wrapper  {
    gradleVersion = "5.0"
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

  val sourcesJar by creating(Jar::class) {
    classifier = "sources"
    from(main.allSource)
    description = "Assembles a JAR of the source code"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  val dokka by getting(DokkaTask::class) {
    dependsOn(main.classesTaskName)
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    // See https://github.com/Kotlin/dokka/issues/196
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
      url = URL("https://docs.gradle.org/${wrapper.map { it.gradleVersion }.get() }/javadoc/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
      url = URL("https://docs.oracle.com/javase/8/docs/api/")
    })
  }

  val javadocJar by creating(Jar::class) {
    dependsOn(dokka)
    from(dokka.outputDirectory)
    classifier = "javadoc"
    description = "Assembles a JAR of the generated Javadoc"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  "assemble" {
    dependsOn(sourcesJar, javadocJar)
  }

  val gitDirtyCheck by creating {
    doFirst {
      val output = ByteArrayOutputStream().use {
        exec {
          commandLine("git", "status", "--porcelain")
          standardOutput = it
        }
        it.toString(Charsets.UTF_8.name()).trim()
      }
      if (output.isNotBlank()) {
        throw GradleException("Workspace is dirty:\n$output")
      }
    }
  }

  val gitTag by creating(Exec::class) {
    description = "Tags the local repository with version ${project.version}"
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    commandLine("git", "tag", "--sign", "-a", project.version, "-m", "Gradle created tag for ${project.version}")
  }

  val pushGitTag by creating(Exec::class) {
    description = "Pushes Git tag ${project.version} to origin"
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    dependsOn(gitTag)
    commandLine("git", "push", "origin", "refs/tags/${project.version}")
  }

  val bintrayUpload by getting {
    dependsOn(gitDirtyCheck, gitTag)
  }

  register("release") {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Publishes the library and pushes up a Git tag for the current commit"
    dependsOn(bintrayUpload, pushGitTag)
  }
}

val publicationName = "gradleTestKotlinExtensions"
publishing {
  publications.invoke {
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
  val bintrayUser = project.findProperty("bintrayUser") as String?
  val bintrayApiKey = project.findProperty("bintrayApiKey") as String?
  user = bintrayUser
  key = bintrayApiKey
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
