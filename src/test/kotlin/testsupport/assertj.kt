package testsupport

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.ThrowableTypeAssert

fun assertSoftly(softly: SoftAssertions.() -> Unit) = SoftAssertions.assertSoftly(softly)

fun assertThatNoSuchFileException(): ThrowableTypeAssert<NoSuchFileException> =
    Assertions.assertThatExceptionOfType(NoSuchFileException::class.java)

fun assertThatFileAlreadyExistsException(): ThrowableTypeAssert<FileAlreadyExistsException> =
    Assertions.assertThatExceptionOfType(FileAlreadyExistsException::class.java)
