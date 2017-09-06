apply {
  plugin("org.jetbrains.kotlin.jvm")
}

val kotlinVersion by rootProject

dependencies {
  implementation(kotlin("stdlib-jre8", kotlinVersion as String))
}
