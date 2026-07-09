package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningUkesverkTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 0.NOK
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 460.NOK
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 10,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 4600.NOK
    }

    test("stengt hele perioden gir 0 i beløp") {
        val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13))
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = periode,
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(StengtPeriode(periode, "Ukestengt")),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 0.NOK
    }

    test("stengt i deler av perioden gir redusert beløp") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(
                StengtPeriode(
                    Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
                    "Uke 2 stengt",
                ),
            ),
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 360.NOK
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningPrisPerUkesverk.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerUkesverk.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = 9500,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningPrisPerUkesverk.beregn(input)
        }
    }
})
