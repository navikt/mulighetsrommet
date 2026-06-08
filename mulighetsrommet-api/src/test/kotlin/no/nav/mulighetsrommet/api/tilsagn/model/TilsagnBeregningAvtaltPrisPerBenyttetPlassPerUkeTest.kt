package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUkeTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input).output.pris shouldBe 100.NOK
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input).output.pris shouldBe 100.NOK
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input).output.pris shouldBe 0.NOK
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input).output.pris shouldBe 460.NOK
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 10,
            prisbetingelser = null,
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input).output.pris shouldBe 4600.NOK
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = 9500,
                prisbetingelser = null,
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(input)
        }
    }
})
