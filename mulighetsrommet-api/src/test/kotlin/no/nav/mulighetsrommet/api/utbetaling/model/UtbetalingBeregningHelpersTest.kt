package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class UtbetalingBeregningHelpersTest : FunSpec({
    test("skal utlede deltakelsesperioder over flere satser") {
        val periodeStart = LocalDate.of(2026, 1, 1)
        val periodeSlutt = LocalDate.of(2026, 2, 1)

        val uke1Start = LocalDate.of(2025, 12, 29)
        val uke2Start = LocalDate.of(2026, 1, 5)
        val uke3Start = LocalDate.of(2026, 1, 12)
        val uke4Start = LocalDate.of(2026, 1, 19)

        val id = UUID.randomUUID()
        val perioder = listOf(
            Periode(periodeStart, uke2Start),
            Periode(uke3Start, periodeSlutt),
        )
        val satser = setOf(
            SatsPeriode(Periode(uke1Start, uke4Start), 1),
            SatsPeriode(Periode(uke4Start, periodeSlutt), 2),
        )
        val calculateFaktor: (Periode) -> BigDecimal = { BigDecimal(1) }

        val deltakelse = UtbetalingBeregningHelpers
            .calculateDeltakelseOutput(id, perioder, satser, calculateFaktor)

        deltakelse.perioder shouldBe setOf(
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(periodeStart, uke2Start),
                faktor = 1.0,
                sats = 1,
            ),
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(uke3Start, uke4Start),
                faktor = 1.0,
                sats = 1,
            ),
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(uke4Start, periodeSlutt),
                faktor = 1.0,
                sats = 2,
            ),
        )
    }

    test("skal beregne riktig beløp for deltakelser med flere perioder og satser") {
        val deltakelseId = UUID.randomUUID()
        val perioder = setOf(
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)),
                faktor = 1.0,
                sats = 100,
            ),
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 20)),
                faktor = 0.5,
                sats = 200,
            ),
        )
        val deltakelser = setOf(
            UtbetalingBeregningOutputDeltakelse(deltakelseId, perioder),
        )

        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(deltakelser)

        // Forventet: 1.0 * 100 + 0.5 * 200 = 200
        belop shouldBe 200
    }
})
