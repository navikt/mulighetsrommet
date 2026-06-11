package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningHeleUkesverkTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("tre ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 11)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("to ukedager gir 1 uke hvis hele uken er i samme måned") {
        TilsagnBeregningPrisPerHeleUkesverk.beregn(
            TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 11)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
                stengt = setOf(),
            ),
        ).output.pris shouldBe 100.NOK

        TilsagnBeregningPrisPerHeleUkesverk.beregn(
            TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
                stengt = setOf(),
            ),
        ).output.pris shouldBe 0.NOK
    }

    test("en ukedag gir 1 uke hvis 3 ukedager er i samme måned") {
        TilsagnBeregningPrisPerHeleUkesverk.beregn(
            TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2)),
                sats = 100.NOK,
                antallPlasser = 1,
                prisbetingelser = null,
                stengt = setOf(),
            ),
        ).output.pris shouldBe 100.NOK
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 0.NOK
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 500.NOK
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 10,
            prisbetingelser = null,
            stengt = setOf(),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 1000.NOK
    }

    test("stengt hele perioden gir 0 i beløp") {
        val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13))
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = periode,
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(StengtPeriode(periode, "Ukestengt")),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 0.NOK
    }

    test("stengt én av to uker gir beløp for én uke") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 20)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(
                StengtPeriode(Periode(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 20)), "Andre uke stengt"),
            ),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("stengt to dager gir full sats") {
        val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.NOK,
            antallPlasser = 1,
            prisbetingelser = null,
            stengt = setOf(
                StengtPeriode(Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 8)), "Stengt i to dager"),
            ),
        )

        TilsagnBeregningPrisPerHeleUkesverk.beregn(input).output.pris shouldBe 100.NOK
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningPrisPerHeleUkesverk.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.NOK,
                antallPlasser = 9500,
                prisbetingelser = null,
                stengt = setOf(),
            )

            TilsagnBeregningPrisPerHeleUkesverk.beregn(input)
        }
    }
})
