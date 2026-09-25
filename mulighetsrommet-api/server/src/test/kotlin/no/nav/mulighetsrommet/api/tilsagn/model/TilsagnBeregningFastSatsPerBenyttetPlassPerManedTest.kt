package no.nav.mulighetsrommet.api.tilsagn.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class TilsagnBeregningFastSatsPerBenyttetPlassPerManedTest : FunSpec({

    test("en plass en måned = sats") {
        val input = TilsagnBeregningFastSatsPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            stengt = setOf(),
            prismodell = PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED,
        )

        TilsagnBeregningFastSatsPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 100.NOK
    }

    test("stengt hele perioden gir 0 i beløp") {
        val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))
        val input = TilsagnBeregningFastSatsPerBenyttetPlassPerManed.Input(
            periode = periode,
            sats = 100.NOK,
            antallPlasser = 1,
            stengt = setOf(StengtPeriode(periode, "Vinterferie")),
            prismodell = PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED,
        )

        TilsagnBeregningFastSatsPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 0.NOK
    }

    test("stengt deler av perioden reduserer beløpet") {
        val input = TilsagnBeregningFastSatsPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            stengt = setOf(
                StengtPeriode(
                    Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 17)),
                    "Vinterferie",
                ),
            ),
            prismodell = PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED,
        )

        // 10/22 ukedager * 100 = 45.45... = 45 NOK
        TilsagnBeregningFastSatsPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 45.NOK
    }

    test("stengt periode utenfor tilsagnsperioden påvirker ikke beløpet") {
        val input = TilsagnBeregningFastSatsPerBenyttetPlassPerManed.Input(
            periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1)),
            sats = 100.NOK,
            antallPlasser = 1,
            stengt = setOf(
                StengtPeriode(Periode.forMonthOf(LocalDate.of(2026, 3, 1)), "Utenfor periode"),
            ),
            prismodell = PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED,
        )

        TilsagnBeregningFastSatsPerBenyttetPlassPerManed.beregn(input).output.pris shouldBe 100.NOK
    }
})
