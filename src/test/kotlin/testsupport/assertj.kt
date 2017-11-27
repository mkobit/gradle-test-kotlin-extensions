package testsupport

import org.assertj.core.api.SoftAssertions

fun assertSoftly(softly: SoftAssertions.() -> Unit) = SoftAssertions.assertSoftly(softly)
