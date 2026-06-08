package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUkeTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 100.NOK
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 100.NOK
    }

    test("tre ukedager gir full sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 100.NOK
    }

    test("to ukedager gir 1 uke hvis hele uken er i samme måned") {
        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(
            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                periode = Periode(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 11)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
            ),
        ).output.pris shouldBe 100.NOK

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(
            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
            ),
        ).output.pris shouldBe 0.NOK
    }

    test("en ukedag gir 1 uke hvis 3 ukedager er i samme måned") {
        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(
            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
            ),
        ).output.pris shouldBe 100.NOK
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 0.NOK
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 500.NOK
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 10,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input).output.pris shouldBe 1000.NOK
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = 9500,
                prisbetingelser = null,
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(input)
        }
    }
})
