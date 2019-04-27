package testsupport.assertj

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.ThrowableAssertAlternative

fun assertSoftly(softly: SoftAssertions.() -> Unit) = SoftAssertions.assertSoftly(softly)

fun assertNoExceptionThrownBy(body: () -> Unit) = Assertions.assertThatCode(body).doesNotThrowAnyException()

fun assertThatNoSuchFileExceptionThrownBy(body: () -> Unit): ThrowableAssertAlternative<NoSuchFileException> =
  Assertions.assertThatExceptionOfType(NoSuchFileException::class.java).isThrownBy(body)

fun assertThatFileAlreadyExistsExceptionThrownBy(body: () -> Unit): ThrowableAssertAlternative<FileAlreadyExistsException> =
  Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java).isThrownBy(body)
