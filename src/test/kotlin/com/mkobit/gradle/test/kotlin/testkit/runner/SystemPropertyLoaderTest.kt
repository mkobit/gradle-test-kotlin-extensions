package com.mkobit.gradle.test.kotlin.testkit.runner

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SystemPropertyLoaderTest {

  private lateinit var loader: SystemPropertyLoader

  @BeforeEach
  internal fun setUp() {
    loader = SystemPropertyLoader()
  }

  @Test
  internal fun `retrieve non-existant property without specifying a default`() {
    val randomProperty = UUID.randomUUID().toString()

    assertThat(loader.getProperty(randomProperty)).isNull()
  }

  @Test
  internal fun `retrieve non-existent property with a default`() {
    val randomProperty = UUID.randomUUID().toString()

    assertThat(loader.getProperty(randomProperty, "default")).isEqualTo("default")
  }

  @Test
  internal fun `retrieve all properties`() {
    SoftAssertions.assertSoftly {
      it.run {
        assertThat(loader.allProperties()).hasEntrySatisfying("java.home") {
          assertThat(it).isNotNull
        }.hasEntrySatisfying("user.name") {
          assertThat(it).isNotNull
        }.hasEntrySatisfying("java.version") {
          assertThat(it).isNotNull
        }.hasEntrySatisfying("java.vendor") {
          assertThat(it).isNotNull
        }
      }
    }
  }
}
