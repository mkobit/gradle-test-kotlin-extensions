apply {
  plugin("org.jetbrains.kotlin.jvm")
  plugin("org.jetbrains.dokka")
}

val kotlinVersion by rootProject

dependencies {
  implementation(kotlin("stdlib-jre8", kotlinVersion as String))
}
