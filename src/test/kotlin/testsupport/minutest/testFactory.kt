package testsupport.minutest

import dev.minutest.TestContextBuilder
import dev.minutest.junit.testFactoryFor
import dev.minutest.rootContext

internal inline fun <reified S : Any?> testFactory(noinline builder: TestContextBuilder<Unit, S>.() -> Unit) =
  testFactoryFor(
    rootContext(builder = builder)
  )
