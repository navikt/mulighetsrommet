package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningUkesverkTest : FunSpec({

    test("en hel uke gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13)),
            sats = 100,
            antallPlasser = 1,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.belop shouldBe 100
    }

    test("fem ukedager gir full sats") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 11)),
            sats = 100,
            antallPlasser = 1,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.belop shouldBe 100
    }

    test("helgedager gir ingen ingenting") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 13)),
            sats = 100,
            antallPlasser = 1,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.belop shouldBe 0
    }

    test("en hel måned gir flere ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100,
            antallPlasser = 1,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.belop shouldBe 460
    }

    test("flere antall plasser øker antall ukesverk") {
        val input = TilsagnBeregningPrisPerUkesverk.Input(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            sats = 100,
            antallPlasser = 10,
        )

        TilsagnBeregningPrisPerUkesverk.beregn(input).output.belop shouldBe 4600
    }
})
