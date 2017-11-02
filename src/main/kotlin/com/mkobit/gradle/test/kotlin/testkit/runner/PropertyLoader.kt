package com.mkobit.gradle.test.kotlin.testkit.runner

/**
 * Loads properties from a source.
 */
internal interface PropertyLoader {

  /**
   * Get the property for the provided key.
   * @param key the key to lookup
   * @param default the default value if the [key] is not found
   * @return the key value or `null` if it does not exist
   */
  fun getProperty(key: String, default: String? = null): String?

  /**
   * Gets all properties.
   * @return all properties available from this loader
   */
  fun allProperties(): Map<String, String>
}

/**
 * Loader backed by the JVM System properties.
 */
internal class SystemPropertyLoader : PropertyLoader {
  override fun getProperty(key: String, default: String?): String? = System.getProperty(key, default)

  @Suppress("UNCHECKED_CAST")
  override fun allProperties(): Map<String, String> = System.getProperties().toMap() as Map<String, String>
}
