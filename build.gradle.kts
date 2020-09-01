import buildsrc.ProjectInfo
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL

plugins {
  `java-library`
  `maven-publish`

  kotlin("jvm") version "1.3.71" // use kotlin 1.4.0 when Gradle updates, as using gradleApi() causes conflicting versions during compilation and failures at runtime
  id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
  id("org.jetbrains.dokka") version "0.10.1" // temporary as 1.4.0-rc is already out

  id("nebula.release") version "15.1.0"
  id("com.github.ben-manes.versions") version "0.29.0"
  id("com.jfrog.bintray") version "1.8.5"
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
  version.set("0.37.2")
}

gradleEnterprise {
  buildScan {
    fun env(key: String): String? = System.getenv(key)

    termsOfServiceAgree = "yes"
    termsOfServiceUrl = "https://gradle.com/terms-of-service"

    link("Repository", ProjectInfo.projectUrl)

    // Env variables from https://docs.github.com/en/actions/configuring-and-managing-workflows/using-environment-variables
    if (env("GITHUB_ACTIONS") == "true") {
      logger.lifecycle("Running in CI environment, setting build scan attributes.")
      tag("CI")
      env("GITHUB_WORKFLOW")?.let { value("GitHub Workflow", it) }
      env("GITHUB_RUN_NUMBER")?.let { value("GitHub Workflow Run Id", it) }
      env("GITHUB_ACTOR")?.let { value("GitHub Actor", it) }
      env("GITHUB_SHA")?.let { value("Revision", it) }
      env("GITHUB_REF")?.let { value("Reference", it) }
      env("GITHUB_REPOSITORY")?.let { value("GitHub Repository", it) }
      env("GITHUB_EVENT_NAME")?.let { value("GitHub Event", it) }
    }
  }
}

repositories {
  jcenter()
  mavenCentral()
}

configurations.all {
  resolutionStrategy.eachDependency {
    when (requested.group) {
      "dev.minutest" -> useVersion("1.11.0")
      "org.junit.jupiter" -> useVersion("5.6.2")
      "org.junit.platform" -> useVersion("1.6.2")
      "io.strikt" -> useVersion("0.26.1")
      "org.apache.logging.log4j" -> useVersion("2.11.2")
      "org.jetbrains.kotlin" -> useVersion("1.3.71") // remove this when Gradle updates to 1.4, see note in plugins section
    }
  }
}

dependencies {
  api(gradleApi())
  api(gradleTestKit())

  implementation("io.github.microutils:kotlin-logging:1.8.3")

  testImplementation(kotlin("reflect"))
  testImplementation("org.assertj:assertj-core:3.17.0")
  testImplementation("io.mockk:mockk:1.10.0")
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

sourceSets {
  main {
    // No Java in main source set
    java.setSrcDirs(emptyList<Any>())
  }
}

tasks {
  wrapper {
    gradleVersion = "6.6.1"
  }

  withType<Jar>().configureEach {
    from(project.projectDir) {
      include("LICENSE.txt")
      into("META-INF")
    }
    manifest {
      attributes(
        mapOf(
          "Build-Revision" to gitCommitSha,
          "Automatic-Module-Name" to ProjectInfo.automaticModuleName,
          "Implementation-Version" to project.version
          // TODO: include Gradle version?
        )
      )
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

  val sourcesJar by registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
    description = "Assembles a JAR of the source code"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  // No Java code, so don't need the javadoc task.
  // Dokka generates our documentation.
  javadoc {
    enabled = false
  }

  dokka {
    dependsOn(sourceSets.main.map { it.classesTaskName })
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    // See https://github.com/Kotlin/dokka/issues/196
    configuration {
      externalDocumentationLink {
        url = URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/")
        packageListUrl = URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/allpackages-index.html")
      }
      externalDocumentationLink {
        url = URL("https://docs.oracle.com/javase/8/docs/api/")
      }
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

  prepare {
    // disable Git upstream checks
    enabled = false
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
  user = findProperty("bintray.user") as String?
  key = findProperty("bintray.key") as String?
  publish = true
  setPublications(publicationName)
  pkg(
    delegateClosureOf<BintrayExtension.PackageConfig> {
      repo = "gradle"
      name = project.name
      userOrg = "mkobit"
      setLabels("gradle", "test", "plugins", "kotlin")
      setLicenses("MIT")
      desc = project.description
      websiteUrl = ProjectInfo.projectUrl
      issueTrackerUrl = ProjectInfo.issuesUrl
      vcsUrl = ProjectInfo.scmUrl
    }
  )
}
