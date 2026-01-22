package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class UtbetalingBeregningPrisPerUkesverkTest : FunSpec({

    context("filtrering av deltakere og satser") {
        test("deltakere med irrelevant status inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(periode, status = DeltakerStatusType.DELTAR),
                createDeltaker(periode, status = DeltakerStatusType.HAR_SLUTTET),
                createDeltaker(periode, status = DeltakerStatusType.FULLFORT),
                createDeltaker(periode, status = DeltakerStatusType.AVBRUTT),
                createDeltaker(periode, status = DeltakerStatusType.IKKE_AKTUELL),
                createDeltaker(periode, status = DeltakerStatusType.FEILREGISTRERT),
                createDeltaker(periode, status = DeltakerStatusType.PABEGYNT_REGISTRERING),
                createDeltaker(periode, status = DeltakerStatusType.SOKT_INN),
                createDeltaker(periode, status = DeltakerStatusType.VENTER_PA_OPPSTART),
            )

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser.map { it.deltakelseId } shouldBe setOf(
                deltakere[0].id,
                deltakere[1].id,
                deltakere[2].id,
                deltakere[3].id,
            )
        }

        test("satser utenfor utbetalingsperioden inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(
                    toAvtaltSats(LocalDate.of(2025, 1, 1), 50.withValuta(Valuta.NOK)),
                    toAvtaltSats(LocalDate.of(2026, 2, 15), 100.withValuta(Valuta.NOK)),
                    toAvtaltSats(LocalDate.of(2026, 3, 1), 150.withValuta(Valuta.NOK)),
                ),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.satser shouldBe setOf(
                SatsPeriode(Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15)), 50),
                SatsPeriode(Periode(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 1)), 100),
            )
        }

        test("deltakere utenfor utbetalingsperioden inkluderes ikke") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))),
            )

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser shouldBe setOf(DeltakelsePeriode(deltakere[0].id, periode))
        }
    }

    context("beregning for pris per ukesverk") {
        test("beløp beregnes fra ukesverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 8)
            val periodeSlutt = LocalDate.of(2023, 6, 15)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 50.withValuta(Valuta.NOK))),
            )

            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(Periode(periodeStart, LocalDate.of(2023, 6, 5))),
                createDeltaker(Periode(periodeStart, periodeMidt)),
            )

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

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
                satser = listOf(toAvtaltSats(periodeStart, 50.withValuta(Valuta.NOK))),
                stengt = listOf(
                    toStengtPeriode(Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)), "Stengt 1"),
                    toStengtPeriode(Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 11)), "Stengt 2"),
                ),
            )

            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(Periode(periodeMidt, periodeSlutt)),
            )

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

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

        test("tillater at gjennomføring starter midt i perioden") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val periodeGjennomforing = Periode(periodeMidt, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periodeGjennomforing,
                satser = listOf(toAvtaltSats(periodeMidt, 50.withValuta(Valuta.NOK))),
                stengt = listOf(),
            )

            val deltakere = listOf(
                createDeltaker(periodeGjennomforing),
                createDeltaker(Periode(periodeSlutt, LocalDate.of(2025, 4, 1))),
            )

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                satser = setOf(SatsPeriode(periodeGjennomforing, 50)),
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakere[0].id, periodeGjennomforing),
                ),
            )
            result.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 100,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 3, 1)),
                                2.0,
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
                satser = listOf(toAvtaltSats(periode.start, 10.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerUkeBeregning.beregn(gjennomforing, deltakere, periode)

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
