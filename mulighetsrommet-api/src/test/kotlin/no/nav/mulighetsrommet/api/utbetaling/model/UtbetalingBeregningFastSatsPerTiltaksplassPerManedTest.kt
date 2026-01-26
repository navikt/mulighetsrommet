package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForForhandsgodkjentPris
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class UtbetalingBeregningFastSatsPerTiltaksplassPerManedTest : FunSpec({
    val sats = 100.withValuta(Valuta.NOK)

    context("filtrering av deltakere og satser") {
        test("deltakere med irrelevant status inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.DELTAR,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.HAR_SLUTTET,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.FULLFORT,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.AVBRUTT,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.IKKE_AKTUELL,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.FEILREGISTRERT,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.PABEGYNT_REGISTRERING,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.SOKT_INN,
                ),
                createDeltaker(
                    periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    status = DeltakerStatusType.VENTER_PA_OPPSTART,
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser.map { it.deltakelseId } shouldBe setOf(
                deltakere[0].id,
                deltakere[1].id,
                deltakere[2].id,
                deltakere[3].id,
            )
        }

        test("deltakere utenfor utbetalingsperioden inkluderes ikke") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(
                    Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1)),
                    deltakelsesmengder = listOf(Deltakelsesmengde(LocalDate.of(2026, 1, 1), 100.0)),
                ),
                createDeltaker(
                    Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)),
                    deltakelsesmengder = listOf(Deltakelsesmengde(LocalDate.of(2026, 1, 1), 100.0)),
                ),
                createDeltaker(
                    Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)),
                    deltakelsesmengder = listOf(Deltakelsesmengde(LocalDate.of(2026, 3, 1), 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser shouldBe setOf(
                DeltakelseDeltakelsesprosentPerioder(
                    deltakere[0].id,
                    listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                ),
            )
        }
    }

    context("beregning for fast sats per tiltaksplass per måned") {
        test("beløp beregnes fra månedsverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 16)
            val periodeSlutt = LocalDate.of(2023, 7, 1)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)

            val deltakere = listOf(
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                ),
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 50.0)),
                ),
                createDeltaker(
                    periode = Periode(periodeStart, periodeMidt),
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 40.0)),
                ),
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(
                        Deltakelsesmengde(periodeStart, 49.0),
                        Deltakelsesmengde(periodeMidt, 50.0),
                    ),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                satser = setOf(SatsPeriode(periode, sats)),
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[0].id,
                        listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[1].id,
                        listOf(DeltakelsesprosentPeriode(periode, 50.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[2].id,
                        listOf(DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 40.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[3].id,
                        listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                            DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                        ),
                    ),
                ),
            )
            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                pris = 300.withValuta(Valuta.NOK),
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[2].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                0.25,
                                sats,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[3].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeMidt),
                                0.25,
                                sats,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt, periodeSlutt),
                                0.5,
                                sats,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("perioder med stengt hos arrangør overstyrer månedsverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)
            val periode = Periode(periodeStart, periodeSlutt)

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(
                periode = periode,
                sats = sats,
                stengt = listOf(toStengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)))),
            )

            val deltakere = listOf(
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                ),
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(
                        Deltakelsesmengde(periodeStart, 49.0),
                        Deltakelsesmengde(periodeMidt, 50.0),
                    ),
                ),
                createDeltaker(
                    periode = Periode(periodeStart, periodeMidt),
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 49.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            val stengtPeriode = Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1))
            result.input shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                satser = setOf(SatsPeriode(periode, sats)),
                stengt = setOf(StengtPeriode(stengtPeriode, "Stengt")),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[0].id,
                        listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[1].id,
                        listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                            DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakere[2].id,
                        listOf(DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0)),
                    ),
                ),
            )
            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                pris = 100.withValuta(Valuta.NOK),
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeStart.plusWeeks(1)),
                                0.25,
                                sats,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt.plusWeeks(1), periodeSlutt),
                                0.25,
                                sats,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeStart.plusWeeks(1)),
                                0.125,
                                sats,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeMidt.plusWeeks(1), periodeSlutt),
                                0.25,
                                sats,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[2].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, periodeStart.plusWeeks(1)),
                                0.125,
                                sats,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("flere stengt hos arrangør perioder i én deltakelses periode") {
            val periode = Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 1))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(
                periode = periode,
                sats = sats,
                stengt = listOf(
                    toStengtPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 2))),
                    toStengtPeriode(Periode(LocalDate.of(2025, 4, 5), LocalDate.of(2025, 4, 19))),
                ),
            )
            val deltakere = listOf(
                createDeltaker(
                    periode = periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                pris = 50.withValuta(Valuta.NOK),
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 4, 2), LocalDate.of(2025, 4, 5)),
                                0.1,
                                sats,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 4, 19), LocalDate.of(2025, 5, 1)),
                                0.4,
                                sats,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("beregnet beløp rundes av til nærmeste hele krone") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 12, 1))
            val deltakelse1 = Periode.fromInclusiveDates(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 7))
            val deltakelse2 = Periode.fromInclusiveDates(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 8))

            val gjennomforing =
                createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 20205.withValuta(Valuta.NOK))
            val deltakere = listOf(
                createDeltaker(
                    periode = deltakelse1,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
                createDeltaker(
                    periode = deltakelse2,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                pris = 9663.withValuta(Valuta.NOK),
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(deltakelse1, 0.21739, 20205.withValuta(Valuta.NOK))),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(deltakelse2, 0.26087, 20205.withValuta(Valuta.NOK))),
                    ),
                ),
            )
        }

        test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
            val periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1))
            val gjennomforing =
                createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 20205.withValuta(Valuta.NOK))

            (2..31).forEach { dayOfMonth ->
                val deltakelsePeriode = Periode(periode.start, periode.start.withDayOfMonth(dayOfMonth))
                val deltakere = listOf(
                    createDeltaker(
                        periode = deltakelsePeriode,
                        deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    ),
                )

                val utbetaling = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

                val tilsagn = TilsagnBeregningPrisPerManedsverk.beregn(
                    TilsagnBeregningPrisPerManedsverk.Input(
                        periode = deltakelsePeriode,
                        sats = 20205.withValuta(Valuta.NOK),
                        antallPlasser = 1,
                        prisbetingelser = null,
                    ),
                )

                utbetaling.output.pris shouldBe tilsagn.output.pris
            }
        }
    }

    context("periode ulik én måned") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))

            val gjennomforing =
                createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 10.withValuta(Valuta.NOK))
            val deltakere = listOf(
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0))),
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0))),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                pris = 40.withValuta(Valuta.NOK),
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10.withValuta(Valuta.NOK))),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 2.0, 10.withValuta(Valuta.NOK))),
                    ),
                ),
            )
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 7, 1))

            val heleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 13))
            val hverdagerUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 11))
            val helgUke36HeleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 13))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(heleUke37, deltakelsesmengder = listOf(Deltakelsesmengde(heleUke37.start, 100.0))),
                createDeltaker(
                    hverdagerUke37,
                    deltakelsesmengder = listOf(Deltakelsesmengde(hverdagerUke37.start, 100.0)),
                ),
                createDeltaker(
                    helgUke36HeleUke37,
                    deltakelsesmengder = listOf(Deltakelsesmengde(helgUke36HeleUke37.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[0].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22581, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[1].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.16129, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[2].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgUke36HeleUke37, 0.29032, sats)),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker ikke beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

            val heleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 14))
            val hverdagerUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 8), LocalDate.of(2025, 9, 12))
            val helgUke36HeleUke37 = Periode.fromInclusiveDates(LocalDate.of(2025, 9, 6), LocalDate.of(2025, 9, 14))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(heleUke37, deltakelsesmengder = listOf(Deltakelsesmengde(heleUke37.start, 100.0))),
                createDeltaker(
                    hverdagerUke37,
                    deltakelsesmengder = listOf(Deltakelsesmengde(hverdagerUke37.start, 100.0)),
                ),
                createDeltaker(
                    helgUke36HeleUke37,
                    deltakelsesmengder = listOf(Deltakelsesmengde(helgUke36HeleUke37.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[0].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22727, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[1].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.22727, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[2].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgUke36HeleUke37, 0.22727, sats)),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. januar 2026") {
        test("50% deltakelse tilsvarer halvt månedsverk") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))

            val gjennomforing =
                createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 100.withValuta(Valuta.NOK))
            val deltakere = listOf(
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 20.0))),
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 50.0))),
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 51.0))),
                createDeltaker(periode, deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 80.0))),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.beregn(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[0].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 0.5, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[1].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 0.5, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[2].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakere[3].id,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                ),
            )
        }
    }
})
