package no.nav.mulighetsrommet.api.avtale.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class PrismodellTest : FunSpec({
    context("findSats") {
        test("rekkefølge på satser har ikke noe å si") {
            val valuta = Valuta.NOK
            val sats2024 = AvtaltSats(LocalDate.of(2024, 1, 1), 1.withValuta(valuta))
            val sats2025 = AvtaltSats(LocalDate.of(2025, 1, 1), 2.withValuta(valuta))
            listOf(sats2024, sats2025).findAvtaltSats(
                LocalDate.of(2025, 5, 5),
            )?.sats shouldBe 2.withValuta(valuta)
            listOf(sats2025, sats2024).findAvtaltSats(
                LocalDate.of(2025, 5, 5),
            )?.sats shouldBe 2.withValuta(valuta)
        }
    }
})
