package no.nav.mulighetsrommet.api.avtale.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Currency
import java.time.LocalDate

class PrismodellTest : FunSpec({
    context("findSats") {
        test("rekkefølge på satser har ikke noe å si") {
            val sats2024 = AvtaltSats(LocalDate.of(2024, 1, 1), 1, Currency.NOK)
            val sats2025 = AvtaltSats(LocalDate.of(2025, 1, 1), 2, Currency.NOK)
            listOf(sats2024, sats2025).findSats(
                LocalDate.of(2025, 5, 5),
            ) shouldBe 2
            listOf(sats2025, sats2024).findSats(
                LocalDate.of(2025, 5, 5),
            ) shouldBe 2
        }
    }
})
