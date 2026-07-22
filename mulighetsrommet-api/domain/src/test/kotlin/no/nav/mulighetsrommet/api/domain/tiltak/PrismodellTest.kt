package no.nav.mulighetsrommet.api.domain.tiltak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class PrismodellTest : FunSpec({
    context("findSats") {
        test("rekkefølge på satser har ikke noe å si") {
            val sats2024 = AvtaltSats(LocalDate.of(2024, 1, 1), 1.NOK)
            val sats2025 = AvtaltSats(LocalDate.of(2025, 1, 1), 2.NOK)

            val prismodell = Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass(
                id = UUID.randomUUID(),
                valuta = Valuta.NOK,
                satser = listOf(sats2024, sats2025),
            )

            prismodell.findAvtaltSats(LocalDate.of(2025, 5, 5))?.sats shouldBe 2.NOK
            prismodell.findAvtaltSats(LocalDate.of(2025, 5, 5))?.sats shouldBe 2.NOK
        }
    }
})
