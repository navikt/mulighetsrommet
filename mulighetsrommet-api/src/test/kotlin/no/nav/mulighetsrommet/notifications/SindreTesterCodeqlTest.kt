package no.nav.mulighetsrommet.notifications

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class SindreTesterCodeqlTest : FunSpec({
    val testKlasse = SindreTesterCodeql()
    test("Skal kaste en ArrayIndexOutOfBoundsException") {
        shouldThrowExactly<ArrayIndexOutOfBoundsException> {
            testKlasse.vulnerableCode()
        }
    }
})
