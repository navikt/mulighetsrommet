package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class TilsagnBeregningUkesverkTest : FunSpec({
    test("en hel uke gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100.withValuta(Valuta.NOK),
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 100.withValuta(Valuta.NOK)
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100.withValuta(Valuta.NOK),
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 100.withValuta(Valuta.NOK)
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100.withValuta(Valuta.NOK),
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 0.withValuta(Valuta.NOK)
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.withValuta(Valuta.NOK),
            antallPlasser = 1,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 460.withValuta(Valuta.NOK)
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100.withValuta(Valuta.NOK),
            antallPlasser = 10,
            prisbetingelser = null,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.pris shouldBe 4600.withValuta(Valuta.NOK)
    }

    test("overflow kaster exception") {
        // overflow i en delberegning for én måned
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerUkesverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 1, 1)),
                sats = 20205.withValuta(Valuta.NOK),
                antallPlasser = Int.MAX_VALUE,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerUkesverk.beregn(input)
        }

        // overflow på summering av 12 måneder
        shouldThrow<ArithmeticException> {
            val input = TilsagnBeregningPrisPerUkesverk.Input(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                sats = 20205.withValuta(Valuta.NOK),
                antallPlasser = 9500,
                prisbetingelser = null,
            )

            TilsagnBeregningPrisPerUkesverk.beregn(input)
        }
    }
})
