package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningForhandsgodkjentTest : FunSpec({

    context("forhåndsgodkjent tilsagn beregning") {
        test("en plass en måned = sats") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 20205
        }

        test("flere plasser en måned") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205,
                antallPlasser = 6,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 121230
        }

        test("en plass halv måned") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 15)),
                sats = 19500,
                antallPlasser = 1,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 9750
        }

        test("flere plasser en og en halv måned") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
                sats = 20205,
                antallPlasser = 10,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 303075
        }

        test("ingen plasser") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 15)),
                sats = 20205,
                antallPlasser = 0,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 0
        }

        test("skuddår/ikke skuddår") {
            val ikkeSkuddar = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)),
                sats = 19500,
                antallPlasser = 1,
            )

            TilsagnBeregningForhandsgodkjent.beregn(ikkeSkuddar).output.belop shouldBe 19500

            val skuddar = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode.fromInclusiveDates(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningForhandsgodkjent.beregn(skuddar).output.belop shouldBe 19599
        }

        test("én dag") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2)),
                sats = 20205,
                antallPlasser = 1,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 606
        }

        test("overflow kaster exception") {
            // overflow i en delberegning for én måned
            shouldThrow<ArithmeticException> {
                val input = TilsagnBeregningForhandsgodkjent.Input(
                    periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                    sats = 20205,
                    antallPlasser = Int.MAX_VALUE,
                )

                TilsagnBeregningForhandsgodkjent.beregn(input)
            }

            // overflow på summering av 12 måneder
            shouldThrow<ArithmeticException> {
                val input = TilsagnBeregningForhandsgodkjent.Input(
                    periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                    sats = 20205,
                    antallPlasser = 9500,
                )

                TilsagnBeregningForhandsgodkjent.beregn(input)
            }
        }

        test("reelt eksempel nr 1") {
            val input = TilsagnBeregningForhandsgodkjent.Input(
                periode = Periode(LocalDate.of(2024, 9, 15), LocalDate.of(2025, 1, 1)),
                sats = 20205,
                antallPlasser = 24,
            )

            TilsagnBeregningForhandsgodkjent.beregn(input).output.belop shouldBe 1711768
        }
    }
})
