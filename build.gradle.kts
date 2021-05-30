import buildsrc.ProjectInfo
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL

plugins {
  `java-library`
  `maven-publish`

  kotlin("jvm") version "1.4.32" // use kotlin 1.4.0 when Gradle updates, as using gradleApi() causes conflicting versions during compilation and failures at runtime
  id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
  id("org.jetbrains.dokka") version "1.4.32"

  id("nebula.release") version "15.3.1"
  id("com.github.ben-manes.versions") version "0.29.0"
}

group = "io.github.mkobit.gradle.test"
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
  version.set("0.41.0")
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
  mavenCentral()
}

dependencies {
  val jupiterVersion = "5.7.2"
  val log4jVersion = "2.14.1"
  val striktVersion = "0.31.0"
  api(gradleApi())
  api(gradleTestKit())

  testImplementation(kotlin("reflect"))
  testImplementation(kotlin("stdlib-jdk7"))
  testImplementation("org.assertj:assertj-core:3.17.0")
  testImplementation("io.mockk:mockk:1.11.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$jupiterVersion")
  testImplementation("dev.minutest:minutest:1.13.0")
  testImplementation("io.strikt:strikt-core:$striktVersion")
  testImplementation("io.strikt:strikt-gradle:$striktVersion")
  testImplementation("io.strikt:strikt-jvm:$striktVersion")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
  testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
  testRuntimeOnly("org.apache.logging.log4j:log4j-jul:$log4jVersion")
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
    gradleVersion = "7.0.2"
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

  withType<DokkaTask>().configureEach {
    dokkaSourceSets {
      configureEach {
        externalDocumentationLink {
          url.set(URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/"))
          packageListUrl.set(URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/allpackages-index.html"))
        }
        externalDocumentationLink {
          url.set(URL("https://docs.oracle.com/javase/8/docs/api/"))
        }
      }
    }
  }

  val javadocJar by registering(Jar::class) {
    from(dokkaJavadoc)
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
//
// bintray {
//  user = findProperty("bintray.user") as String?
//  key = findProperty("bintray.key") as String?
//  publish = true
//  setPublications(publicationName)
//  pkg(
//    delegateClosureOf<BintrayExtension.PackageConfig> {
//      repo = "gradle"
//      name = project.name
//      userOrg = "mkobit"
//      setLabels("gradle", "test", "plugins", "kotlin")
//      setLicenses("MIT")
//      desc = project.description
//      websiteUrl = ProjectInfo.projectUrl
//      issueTrackerUrl = ProjectInfo.issuesUrl
//      vcsUrl = ProjectInfo.scmUrl
//    }
//  )
// }
