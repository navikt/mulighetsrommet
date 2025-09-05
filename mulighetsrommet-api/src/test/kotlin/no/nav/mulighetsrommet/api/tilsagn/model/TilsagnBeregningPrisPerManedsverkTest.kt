package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningPrisPerManedsverkTest : FunSpec({

    test("en plass en måned = sats") {
        val input = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 20205,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 20205
    }

    test("flere plasser en måned") {
        val input = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
            sats = 20205,
            antallPlasser = 6,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 121230
    }

    test("en plass halv måned") {
        val input = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 15)),
            sats = 19500,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 9750
    }

    test("flere plasser en og en halv måned") {
        val input = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
            sats = 20205,
            antallPlasser = 10,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 303075
    }

    test("ingen plasser") {
        val input = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
            sats = 20205,
            antallPlasser = 0,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 0
    }

    test("skuddår/ikke skuddår") {
        val ikkeSkuddar = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)),
            sats = 20205,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerManedsverk.beregn(ikkeSkuddar).output.belop shouldBe 20205

        val skuddar = TilsagnBeregningPrisPerManedsverk.Input(
            periode = Periode.fromInclusiveDates(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28)),
            sats = 20205,
            antallPlasser = 1,
            prisbetingelser = null,
        )

        // 20/21 * 20205 = 19242.85
        TilsagnBeregningPrisPerManedsverk.beregn(skuddar).output.belop shouldBe 19508
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205,
                antallPlasser = 9500,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input)
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("én virkedag tilsvarer beløpet for en dag i løpet av perioden") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 3)),
                sats = 20205,
                antallPlasser = 1,
                prisbetingelser = null,
            )

            // 1/31 * 20205 = 651.7
            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 652
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("én virkedag tilsvarer beløpet for en ukedag i løpet av perioden") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2)),
                sats = 20205,
                antallPlasser = 1,
                prisbetingelser = null,
            )

            // 1/21 * 20205 = 962.1
            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 962
        }
    }
})
