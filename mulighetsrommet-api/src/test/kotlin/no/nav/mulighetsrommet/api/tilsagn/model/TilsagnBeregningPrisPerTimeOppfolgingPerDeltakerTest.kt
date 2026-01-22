package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class TilsagnBeregningPrisPerTimeOppfolgingPerDeltakerTest : FunSpec({
    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.withValuta(Valuta.NOK),
                antallPlasser = Int.MAX_VALUE,
                antallTimerOppfolgingPerDeltaker = 1,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.withValuta(Valuta.NOK),
                antallPlasser = 9500,
                antallTimerOppfolgingPerDeltaker = 987_455,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(input)
        }
    }

    test("periode påvirker ikke beregning") {
        val input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 2.withValuta(Valuta.NOK),
            antallPlasser = 2,
            antallTimerOppfolgingPerDeltaker = 2,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(input).output.pris shouldBe 8.withValuta(Valuta.NOK)
        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
            input.copy(periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))),
        ).output.pris shouldBe 8.withValuta(Valuta.NOK)
        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
            input.copy(periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))),
        ).output.pris shouldBe 8.withValuta(Valuta.NOK)
    }
})
