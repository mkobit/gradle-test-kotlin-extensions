package testsupport.strikt

import strikt.api.Assertion

fun Assertion.Builder<ByteArray>.isEmpty(): Assertion.Builder<ByteArray> =
  assertThat("is empty") { it.isEmpty() }
