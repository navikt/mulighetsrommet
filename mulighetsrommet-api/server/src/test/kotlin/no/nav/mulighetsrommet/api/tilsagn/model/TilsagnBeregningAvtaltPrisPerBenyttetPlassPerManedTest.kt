package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManedTest : FunSpec({

    test("en plass en måned = sats") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 20205.NOK
    }

    test("flere plasser en måned") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 20205.NOK,
            antallPlasser = 6,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 121230.NOK
    }

    test("en plass halv måned") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 15)),
            sats = 19500.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 9750.NOK
    }

    test("flere plasser en og en halv måned") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
            sats = 20205.NOK,
            antallPlasser = 10,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 303075.NOK
    }

    test("ingen plasser") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
            sats = 20205.NOK,
            antallPlasser = 0,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 0.NOK
    }

    test("skuddår/ikke skuddår") {
        val ikkeSkuddar = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)),
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        // 28 / 28 * 20205 = 20205
        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(ikkeSkuddar).output.pris shouldBe 20205.NOK

        val skuddar = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28)),
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        // 28 / 29 * 20205 = 19508.27...
        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(skuddar).output.pris shouldBe 19508.NOK
    }

    test("stengt hele perioden gir 0 i beløp") {
        val periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1))
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = periode,
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(StengtPeriode(periode, "Juleferie")),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 0.NOK
    }

    test("stengt halve perioden halverer beløpet") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 29)),
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(StengtPeriode(Periode.forMonthOf(LocalDate.of(2024, 2, 1)), "Vinterstengt")),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 20205.NOK
    }

    test("stengt periode som ikke overlapper med tilsagnperiode påvirker ikke beløpet") {
        val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 20205.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(StengtPeriode(Periode.forMonthOf(LocalDate.of(2024, 3, 1)), "Utenfor periode")),
        )

        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 20205.NOK
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = 9500,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input)
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("én virkedag tilsvarer beløpet for en dag i løpet av perioden") {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
                periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 3)),
                sats = 20205.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
                stengt = setOf(),
            )

            // 1/31 * 20205 = 651.7
            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 652.NOK
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("én virkedag tilsvarer beløpet for en ukedag i løpet av perioden") {
            val input = TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
                periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2)),
                sats = 20205.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
                stengt = setOf(),
            )

            // 1/21 * 20205 = 962.1
            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 962.NOK
        }
    }
})
