package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class UtbetalingBeregningPrisPerUkesverkTest : FunSpec({

    context("beregning for pris per ukesverk") {
        test("beløp beregnes fra ukesverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 8)
            val periodeSlutt = LocalDate.of(2023, 6, 15)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 50)),
            )

            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(Periode(periodeStart, LocalDate.of(2023, 6, 5))),
                createDeltaker(Periode(periodeStart, periodeMidt)),
            )

            val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 50)),
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakere[0].id, periode),
                    DeltakelsePeriode(deltakere[1].id, Periode(periodeStart, LocalDate.of(2023, 6, 5))),
                    DeltakelsePeriode(deltakere[2].id, Periode(periodeStart, periodeMidt)),
                ),
            )
            result.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 170,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 50)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, LocalDate.of(2023, 6, 5)),
                                0.4,
                                50,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[2].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                1.0,
                                50,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("perioder med stengt hos arrangør overstyrer ukesverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 8)
            val periodeSlutt = LocalDate.of(2025, 2, 15)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 50)),
                stengt = listOf(
                    toStengtPeriode(Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)), "Stengt 1"),
                    toStengtPeriode(Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 11)), "Stengt 2"),
                ),
            )

            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(Periode(periodeMidt, periodeSlutt)),
            )

            val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 50)),
                stengt = setOf(
                    StengtPeriode(Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)), "Stengt 1"),
                    StengtPeriode(Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 11)), "Stengt 2"),
                ),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakere[0].id, periode),
                    DeltakelsePeriode(deltakere[1].id, Periode(periodeMidt, periodeSlutt)),
                ),
            )
            result.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 110,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 2, 5), LocalDate.of(2025, 2, 10)),
                                0.6,
                                50,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 2, 11), LocalDate.of(2025, 2, 15)),
                                0.8,
                                50,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 2, 11), LocalDate.of(2025, 2, 15)),
                                0.8,
                                50,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    context("periode ulik én uke") {
        test("en deltaker over 6 uker") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 12))
            periode.getDurationInDays() shouldBe 6 * 7

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10)),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 60,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 6.0, 10)),
                    ),
                ),
            )
        }
    }
})
