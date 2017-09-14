package testsupport

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

class BooleanArgumentsProvider : ArgumentsProvider {
  override fun provideArguments(context: ExtensionContext): Stream<out Arguments> = Stream.of(
    Arguments.of(true),
    Arguments.of(false)
  )
}

@ArgumentsSource(BooleanArgumentsProvider::class)
annotation class BooleanSource
