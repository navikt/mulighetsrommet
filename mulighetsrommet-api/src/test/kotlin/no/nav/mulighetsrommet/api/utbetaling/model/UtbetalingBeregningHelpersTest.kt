package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import no.nav.mulighetsrommet.model.Periode

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

    test("beregner hele ukesverk og utleder sammenhengede deltakelsesperioder fordelt på satsene") {
        val deltakerId = UUID.randomUUID()
        val deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))

        val sats1Periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
        val sats2Periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 16))
        val sats3Periode = Periode(LocalDate.of(2025, 1, 16), LocalDate.of(2025, 2, 1))
        val satser = setOf(
            SatsPeriode(sats1Periode, 100),
            SatsPeriode(sats2Periode, 150),
            SatsPeriode(sats3Periode, 200),
        )

        val deltakelse = DeltakelsePeriode(deltakerId, deltakelsePeriode)

        val resultat = UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
            deltakelse,
            satser,
            emptyList(),
        )

        resultat.perioder shouldBe setOf(
            // Uke 1, 2 og 3 innenfor første sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 20)),
                faktor = 3.0,
                sats = 100,
            ),
            // Uke 4 og 5 innenfor tredje sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)),
                faktor = 2.0,
                sats = 200,
            ),
        )
    }

    test("beregner hele ukesverk og trekker fra stengte perioder 1") {
        val deltakerId = UUID.randomUUID()
        val deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))
        val deltakelse = DeltakelsePeriode(deltakerId, deltakelsePeriode)

        val sats1Periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
        val sats2Periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 17))
        val sats3Periode = Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 2, 1))
        val satser = setOf(
            SatsPeriode(sats1Periode, 100),
            SatsPeriode(sats2Periode, 150),
            SatsPeriode(sats3Periode, 200),
        )

        val stengt1Periode = Periode(LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 12))
        val stengt2Periode = Periode(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 16))
        val stengt = listOf(stengt1Periode, stengt2Periode)

        val resultat = UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
            deltakelse,
            satser,
            stengt,
        )

        resultat.perioder shouldBe setOf(
            // Uke 1 innenfor første sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2)),
                faktor = 1.0,
                sats = 100,
            ),
            // Uke 2 stengt, uke 3 ble også påbegynt innenfor første sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 14)),
                faktor = 1.0,
                sats = 100,
            ),
            // Andre halvdel av uke 3 telles ikke, men er innenfor andre sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 16), LocalDate.of(2025, 1, 20)),
                faktor = 0.0,
                sats = 150,
            ),
            // Uke 4 og 5 sammenhengende innenfor tredje sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1)),
                faktor = 2.0,
                sats = 200,
            ),
        )
    }

    test("beregner hele ukesverk og trekker fra stengte perioder 2") {
        val deltakerId = UUID.randomUUID()
        val deltakelsePeriode = Periode(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 11, 1))
        val deltakelse = DeltakelsePeriode(deltakerId, deltakelsePeriode)

        val sats1Periode = Periode(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 8))
        val sats2Periode = Periode(LocalDate.of(2025, 10, 8), LocalDate.of(2025, 11, 1))
        val satser = setOf(SatsPeriode(sats1Periode, 100), SatsPeriode(sats2Periode, 150))

        val stengt1Periode = Periode(LocalDate.of(2025, 10, 6), LocalDate.of(2025, 10, 9))
        val stengt2Periode = Periode(LocalDate.of(2025, 10, 20), LocalDate.of(2025, 10, 27))
        val stengt = listOf(stengt1Periode, stengt2Periode)

        val resultat = UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
            deltakelse,
            satser,
            stengt,
        )

        resultat.perioder shouldBe setOf(
            // Uke 1 med første sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 6)),
                faktor = 1.0,
                sats = 100,
            ),
            // Uke 2 og 3 med andre sats, siden starten av uken av stengt
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 10, 9), LocalDate.of(2025, 10, 20)),
                faktor = 2.0,
                sats = 150,
            ),
            // Uke 4 stengt, uke 5 med andre sats
            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                periode = Periode(LocalDate.of(2025, 10, 27), LocalDate.of(2025, 11, 1)),
                faktor = 1.0,
                sats = 150,
            ),
        )
    }
})
