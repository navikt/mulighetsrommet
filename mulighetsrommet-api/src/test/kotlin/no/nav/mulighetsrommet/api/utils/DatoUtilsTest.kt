package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import java.time.LocalDate

class DatoUtilsTest : FunSpec({
    test("Skal formatere til europeisk datoformat") {
        val dato = LocalDate.of(2021, 1, 1)
        dato.formaterDatoTilEuropeiskDatoformat() shouldBe "01.01.2021"
    }

    test("Skal returnere null hvis dato er null") {
        val dato: LocalDate? = null
        dato.formaterDatoTilEuropeiskDatoformat() shouldBe null
    }
})
