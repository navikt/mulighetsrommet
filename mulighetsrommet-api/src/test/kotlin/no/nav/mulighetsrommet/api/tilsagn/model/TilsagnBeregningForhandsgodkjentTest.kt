package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningForhandsgodkjentTest : FunSpec({

    context("forhåndsgodkjent tilsagn beregning") {
        test("en plass en måned = sats") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 20205
        }

        test("flere plasser en måned") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = 6,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 121230
        }

        test("en plass halv måned") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 15)),
                sats = 19500,
                antallPlasser = 1,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 9750
        }

        test("flere plasser en og en halv måned") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
                sats = 20205,
                antallPlasser = 10,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 303075
        }

        test("ingen plasser") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
                sats = 20205,
                antallPlasser = 0,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 0
        }

        test("skuddår/ikke skuddår") {
            val ikkeSkuddar = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)),
                sats = 19500,
                antallPlasser = 1,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(ikkeSkuddar).output.belop shouldBe 19500

            val skuddar = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(skuddar).output.belop shouldBe 19508
        }

        test("én dag") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 652
        }

        test("overflow kaster exception") {
            // overflow i en delberegning for én måned
            shouldThrow<ArithmeticException> {
                val input = TilsagnBeregningPrisPerManedsverk.Input(
                    periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                    sats = 20205,
                    antallPlasser = Int.MAX_VALUE,
                )

                TilsagnBeregningPrisPerManedsverk.beregn(input)
            }

            // overflow på summering av 12 måneder
            shouldThrow<ArithmeticException> {
                val input = TilsagnBeregningPrisPerManedsverk.Input(
                    periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                    sats = 20205,
                    antallPlasser = 9500,
                )

                TilsagnBeregningPrisPerManedsverk.beregn(input)
            }
        }

        test("reelt eksempel nr 1") {
            val input = TilsagnBeregningPrisPerManedsverk.Input(
                periode = Periode(LocalDate.of(2024, 9, 15), LocalDate.of(2025, 1, 1)),
                sats = 20205,
                antallPlasser = 24,
            )

            // Merk at dette er annerledes (mer mer presisjon) enn det Arena ville lagd = 1_711_768
            TilsagnBeregningPrisPerManedsverk.beregn(input).output.belop shouldBe 1_713_384
        }
    }
})
