package testsupport.assertj

import org.assertj.core.api.Assertions

fun assertNoExceptionThrownBy(body: () -> Unit) = Assertions.assertThatCode(body).doesNotThrowAnyException()
