package buildsrc

object DependencyInfo {
  private const val junitPlatformVersion = "1.4.1"
  private const val junitJupiterVersion = "5.4.1"
  private const val junit5Log4jVersion = "2.11.2"
  private const val kotlinLoggingVersion = "1.6.26"

  const val assertJCore = "org.assertj:assertj-core:3.12.2"
  const val mockito = "org.mockito:mockito-core:2.27.0"
  const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
  private val junitPlatformRunner = junitPlatform("runner")
  private val junitJupiterApi = junitJupiter("api")
  private val junitJupiterEngine = junitJupiter("engine")
  private val junitJupiterParams = junitJupiter("params")
  const val kotlinLogging = "io.github.microutils:kotlin-logging:$kotlinLoggingVersion"
  private val log4jCore = log4j("core")
  private val log4jJul = log4j("jul")

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
