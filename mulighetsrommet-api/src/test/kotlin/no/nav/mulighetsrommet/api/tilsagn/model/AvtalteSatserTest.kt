package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import java.time.LocalDate

class AvtalteSatserTest : FunSpec({
    test("rekkefølge på satser har ikke noe å si") {
        val sats2024 = AvtaltSats(
            gjelderFra = LocalDate.of(2024, 1, 1),
            sats = 1,
        )
        val sats2025 = AvtaltSats(
            gjelderFra = LocalDate.of(2025, 1, 1),
            sats = 2,
        )
        AvtalteSatser.findSats(
            listOf(sats2024, sats2025),
            LocalDate.of(2025, 5, 5),
        ) shouldBe 2
        AvtalteSatser.findSats(
            listOf(sats2025, sats2024),
            LocalDate.of(2025, 5, 5),
        ) shouldBe 2
    }
})
