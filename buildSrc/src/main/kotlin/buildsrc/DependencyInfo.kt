package buildsrc

@Suppress("UNUSED", "MemberVisibilityCanBePrivate")
object DependencyInfo {
  const val junitPlatformVersion = "1.3.1"
  const val junitJupiterVersion = "5.3.1"
  const val junit5Log4jVersion = "2.11.1"
  const val kotlinLoggingVersion = "1.6.10"
  const val slf4jVersion = "1.7.25"

  const val assertJCore = "org.assertj:assertj-core:3.11.1"
  const val junitPioneer = "org.junit-pioneer:junit-pioneer:0.2.2"
  const val mockito = "org.mockito:mockito-core:2.23.0"
  const val mockitoKotlin = "com.nhaarman:mockito-kotlin:1.6.0"
  val junitPlatformRunner = junitPlatform("runner")
  val junitJupiterApi = junitJupiter("api")
  val junitJupiterEngine = junitJupiter("engine")
  val junitJupiterParams = junitJupiter("params")
  const val kotlinLogging = "io.github.microutils:kotlin-logging:$kotlinLoggingVersion"
  val log4jCore = log4j("core")
  val log4jJul = log4j("jul")

  val junitTestImplementationArtifacts = listOf(
      junitPlatformRunner,
      junitJupiterApi,
      junitJupiterParams
  )

  val junitTestRuntimeOnlyArtifacts = listOf(
      junitJupiterEngine,
      log4jCore,
      log4jJul
  )

  fun junitJupiter(module: String) = "org.junit.jupiter:junit-jupiter-$module:$junitJupiterVersion"
  fun junitPlatform(module: String) = "org.junit.platform:junit-platform-$module:$junitPlatformVersion"
  fun log4j(module: String) = "org.apache.logging.log4j:log4j-$module:$junit5Log4jVersion"
}
