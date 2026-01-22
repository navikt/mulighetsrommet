package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class UtbetalingBeregningPrisPerManedsverkTest : FunSpec({

    context("filtrering av deltakere og satser") {
        test("deltakere med irrelevant status inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
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
                createDeltaker(periode, status = DeltakerStatusType.VENTELISTE),
                createDeltaker(periode, status = DeltakerStatusType.UTKAST_TIL_PAMELDING),
                createDeltaker(periode, status = DeltakerStatusType.AVBRUTT_UTKAST),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser.map { it.deltakelseId } shouldBe setOf(
                deltakere[0].id,
                deltakere[1].id,
                deltakere[2].id,
                deltakere[3].id,
            )
        }

        test("satser utenfor utbetalingsperioden inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(
                    toAvtaltSats(LocalDate.of(2025, 1, 1), 50.withValuta(Valuta.NOK)),
                    toAvtaltSats(LocalDate.of(2026, 2, 15), 100.withValuta(Valuta.NOK)),
                    toAvtaltSats(LocalDate.of(2026, 3, 1), 150.withValuta(Valuta.NOK)),
                ),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.satser shouldBe setOf(
                SatsPeriode(Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15)), 50),
                SatsPeriode(Periode(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 1)), 100),
            )
        }

        test("deltakere utenfor utbetalingsperioden inkluderes ikke") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser shouldBe setOf(DeltakelsePeriode(deltakere[0].id, periode))
        }
    }

    context("enkel beregning full og halv") {
        test("deltakere med ulike perioder beregnes korrekt") {
            val periodeStart = LocalDate.of(2026, 4, 1)
            val periodeMidt = LocalDate.of(2026, 4, 16)
            val periodeSlutt = LocalDate.of(2026, 5, 1)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(Periode(periodeStart, periodeMidt)),
                createDeltaker(Periode(periodeMidt, periodeSlutt)),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakere[0].id, periode),
                    DeltakelsePeriode(deltakere[1].id, Periode(periodeStart, periodeMidt)),
                    DeltakelsePeriode(deltakere[2].id, Periode(periodeMidt, periodeSlutt)),
                ),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 200,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                periode,
                                1.0,
                                100,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                0.5,
                                100,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[2].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt, periodeSlutt),
                                0.5,
                                100,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    context("stengt perioder") {
        test("én deltaker full periode, stengt start til midt") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 100.withValuta(Valuta.NOK))),
                stengt = listOf(toStengtPeriode(Periode(periodeStart, periodeMidt))),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                deltakelser = setOf(DeltakelsePeriode(deltakere[0].id, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt, periodeSlutt),
                                0.5,
                                100,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("én deltaker full periode, stengt halve") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 100.withValuta(Valuta.NOK))),
                stengt = listOf(toStengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)))),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeStart.plusWeeks(1)),
                                0.25,
                                100,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt.plusWeeks(1), periodeSlutt),
                                0.25,
                                100,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("flere stengt hos arrangør perioder i én deltakelses periode") {
            val periode = Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
                stengt = listOf(
                    toStengtPeriode(Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 2))),
                    toStengtPeriode(Periode(LocalDate.of(2023, 4, 5), LocalDate.of(2023, 4, 19))),
                ),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2023, 4, 2), LocalDate.of(2023, 4, 5)),
                                0.1,
                                100,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2023, 4, 19), LocalDate.of(2023, 5, 1)),
                                0.4,
                                100,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
        val periode = Periode.forMonthOf(LocalDate.of(2023, 3, 1))

        val gjennomforing = createGjennomforingForPrisPerManedsverk(
            periode = periode,
            satser = listOf(toAvtaltSats(periode.start, 20205.withValuta(Valuta.NOK))),
        )

        (2..31).forEach { dayOfMonth ->
            val deltakelsePeriode = Periode(periode.start, periode.start.withDayOfMonth(dayOfMonth))
            val deltakere = listOf(createDeltaker(deltakelsePeriode))

            val utbetaling = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            val tilsagn = TilsagnBeregningPrisPerManedsverk.beregn(
                TilsagnBeregningPrisPerManedsverk.Input(
                    periode = deltakelsePeriode,
                    sats = 20205.withValuta(Valuta.NOK),
                    antallPlasser = 1,
                    prisbetingelser = null,
                ),
            )

            utbetaling.output.belop shouldBe tilsagn.output.pris.belop
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(periode),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                ),
            )
        }

        test("helgedager før og etter en periode på fem hverdager påvirker beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 7, 1))

            val heleUke = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 13))
            val hverdagerUke = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 11))
            val helgOgHeleUke = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 13))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(heleUke),
                createDeltaker(hverdagerUke),
                createDeltaker(helgOgHeleUke),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[0].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            heleUke,
                            0.22581,
                            100,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[1].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            hverdagerUke,
                            0.16129,
                            100,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[2].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            helgOgHeleUke,
                            0.29032,
                            100,
                        ),
                    ),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(periode),
                createDeltaker(periode),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                periode,
                                2.0,
                                10,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                periode,
                                2.0,
                                10,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("helgedager før og etter en periode på fem hverdager påvirker ikke beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

            val heleUke = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 14))
            val hverdagerUke = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 12))
            val helgOgHeleUke = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 6), LocalDate.of(2025, 9, 14))

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100.withValuta(Valuta.NOK))),
            )
            val deltakere = listOf(
                createDeltaker(heleUke),
                createDeltaker(hverdagerUke),
                createDeltaker(helgOgHeleUke),
            )

            val result = PrisPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[0].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            heleUke,
                            0.22727,
                            100,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[1].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            hverdagerUke,
                            0.22727,
                            100,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[2].id,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            helgOgHeleUke,
                            0.22727,
                            100,
                        ),
                    ),
                ),
            )
        }
    }
})
