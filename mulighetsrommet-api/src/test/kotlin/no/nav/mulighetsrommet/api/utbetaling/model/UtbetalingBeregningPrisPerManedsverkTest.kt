package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class UtbetalingBeregningPrisPerManedsverkTest : FunSpec({
    context("enkel beregning full og halv") {
        test("én deltaker full periode") {
            val periode = Periode(LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1))
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId, periode),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = setOf(),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 100,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, 100)),
                    ),
                ),
            )
        }

        test("én deltaker halv periode") {
            val periodeStart = LocalDate.of(2026, 4, 1)
            val periodeMidt = LocalDate.of(2026, 4, 16)
            val periodeSlutt = LocalDate.of(2026, 5, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 700_000)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId, Periode(periodeStart, periodeMidt)),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 700_000)),
                stengt = setOf(),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, Periode(periodeStart, periodeMidt))),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 350_000,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                0.5,
                                700_000,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("to deltakere med halve perioder") {
            val periodeStart = LocalDate.of(2026, 4, 1)
            val periodeMidt = LocalDate.of(2026, 4, 16)
            val periodeSlutt = LocalDate.of(2026, 5, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 202)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId1, Periode(periodeStart, periodeMidt)),
                createDeltaker(deltakerId2, Periode(periodeMidt, periodeSlutt)),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 202)),
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeMidt)),
                    DeltakelsePeriode(deltakerId2, Periode(periodeMidt, periodeSlutt)),
                ),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 202,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId1,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                0.5,
                                202,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId2,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt, periodeSlutt),
                                0.5,
                                202,
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
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 100)),
                stengt = listOf(toStengtPeriode(Periode(periodeStart, periodeMidt))),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
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
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periodeStart, 100)),
                stengt = listOf(toStengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)))),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerManedsverk.Input(
                satser = setOf(SatsPeriode(periode, 100)),
                stengt = setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
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
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
                stengt = listOf(
                    toStengtPeriode(Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 2))),
                    toStengtPeriode(Periode(LocalDate.of(2023, 4, 5), LocalDate.of(2023, 4, 19))),
                ),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
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
            satser = listOf(toAvtaltSats(periode.start, 20205)),
        )

        (2..31).forEach { dayOfMonth ->
            val deltakelsePeriode = Periode(periode.start, periode.start.withDayOfMonth(dayOfMonth))

            val deltakere = listOf(createDeltaker(periode = deltakelsePeriode))

            val utbetaling = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            val tilsagn = TilsagnBeregningPrisPerManedsverk.beregn(
                TilsagnBeregningPrisPerManedsverk.Input(
                    periode = deltakelsePeriode,
                    sats = 20205,
                    valuta = Valuta.NOK,
                    antallPlasser = 1,
                    prisbetingelser = null,
                ),
            )

            utbetaling.output.belop shouldBe tilsagn.output.belop
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId1, periode),
                createDeltaker(deltakerId2, periode),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId1,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId2,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                ),
            )
        }

        test("helgedager før og etter en periode på fem hverdager påvirker beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 7, 1))

            val heleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 13))
            val hverdagerUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 11))

            val helgFraUke36OgHeleUke37 =
                Periode.fromInclusiveDates(LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 13))

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            val deltakerId3 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId1, heleUke37),
                createDeltaker(deltakerId2, hverdagerUke37),
                createDeltaker(deltakerId3, helgFraUke36OgHeleUke37),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId1,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22581, 100)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId2,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.16129, 100)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId3,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgFraUke36OgHeleUke37, 0.29032, 100)),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId1, periode),
                createDeltaker(deltakerId2, periode),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId1,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId2,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10)),
                    ),
                ),
            )
        }

        test("helgedager før og etter en periode på fem hverdager påvirker ikke beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

            val heleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 14))
            val hverdagerUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 12))
            val helgFraUke36OgHeleUke37 =
                Periode.fromInclusiveDates(LocalDate.of(2025, 9, 6), LocalDate.of(2025, 9, 14))

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            val deltakerId3 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerManedsverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
            )
            val deltakere = listOf(
                createDeltaker(deltakerId1, heleUke37),
                createDeltaker(deltakerId2, hverdagerUke37),
                createDeltaker(deltakerId3, helgFraUke36OgHeleUke37),
            )

            val result = PrisPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId1,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22727, 100)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId2,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.22727, 100)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId3,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgFraUke36OgHeleUke37, 0.22727, 100)),
                ),
            )
        }
    }
})
