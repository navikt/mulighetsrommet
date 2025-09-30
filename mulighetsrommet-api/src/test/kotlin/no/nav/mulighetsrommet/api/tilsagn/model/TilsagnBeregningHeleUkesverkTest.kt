package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningHeleUkesverkTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 100
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 100
    }

    test("tre ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 11)),
            sats = 100,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 100
    }

    test("to ukedager gir 1 uke hvis hele uken er i samme måned") {
        TilsagnBeregningPrisPerHeleUkesverk.beregn(
            TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 11)),
                sats = 100,
                antallPlasser = 1,
                prisbetingelser = null,
            ),
        ).output.belop shouldBe 100

        TilsagnBeregningPrisPerHeleUkesverk.beregn(
            TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3)),
                sats = 100,
                antallPlasser = 1,
                prisbetingelser = null,
            ),
        ).output.belop shouldBe 0
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 0
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 500
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100,
            antallPlasser = 10,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.belop shouldBe 1000
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerHeleUkesverk.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205,
                antallPlasser = 9500,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerHeleUkesverk.beregn(input)
        }
    }
})
