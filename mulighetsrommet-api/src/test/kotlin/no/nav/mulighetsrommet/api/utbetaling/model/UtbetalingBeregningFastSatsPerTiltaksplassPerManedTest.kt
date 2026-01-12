package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForForhandsgodkjentPris
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class UtbetalingBeregningFastSatsPerTiltaksplassPerManedTest : FunSpec({
    val sats = 100

    context("beregning for fast sats per tiltaksplass per måned") {
        test("beløp beregnes fra månedsverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 16)
            val periodeSlutt = LocalDate.of(2023, 7, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 50.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = Periode(periodeStart, periodeMidt),
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 40.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 25,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeMidt),
                                        0.25,
                                        sats,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(
                                Deltakelsesmengde(periodeStart, 49.0),
                                Deltakelsesmengde(periodeMidt, 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 75,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
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
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                        ),
                        createDeltaker(
                            id = deltakerId2,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 49.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 150,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        1.0,
                                        sats,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        0.5,
                                        sats,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(
                                Deltakelsesmengde(periodeStart, 49.0),
                                Deltakelsesmengde(periodeMidt, 50.0),
                            ),
                        ),
                        createDeltaker(
                            id = deltakerId2,
                            periode = Periode(periodeStart, periodeMidt),
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 49.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
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
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeMidt),
                                        0.25,
                                        sats,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { deltakere, expectedOutput ->
                val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)

                val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

                result.output shouldBe expectedOutput
            }
        }

        test("perioder med stengt hos arrangør overstyrer månedsverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    listOf(toStengtPeriode(Periode(periodeStart, periodeMidt))),
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeMidt, periodeSlutt),
                                        0.5,
                                        sats,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(toStengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)))),
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 100.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
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
                        ),
                    ),
                ),
                row(
                    listOf(toStengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)))),
                    listOf(
                        createDeltaker(
                            id = deltakerId1,
                            periode = periode,
                            deltakelsesmengder = listOf(
                                Deltakelsesmengde(periodeStart, 49.0),
                                Deltakelsesmengde(periodeMidt, 50.0),
                            ),
                        ),
                        createDeltaker(
                            id = deltakerId2,
                            periode = Periode(periodeStart, periodeMidt),
                            deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 49.0)),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
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
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeStart.plusWeeks(1)),
                                        0.125,
                                        sats,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { stengt, deltakere, expectedOutput ->
                val gjennomforing = createGjennomforingForForhandsgodkjentPris(
                    periode = periode,
                    sats = sats,
                    stengt = stengt,
                )

                val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

                result.output shouldBe expectedOutput
            }
        }

        test("månedsverk blir beregnet med tilstrekkelig presisjon") {
            val periodeStart = LocalDate.of(2026, 1, 1)
            val periodeMidt = LocalDate.of(2026, 1, 16)
            val periodeSlutt = LocalDate.of(2026, 2, 1)
            val periode = Periode(periodeStart, periodeSlutt)
            val stengtPeriodeStart = periodeStart.plusWeeks(1)
            val stengtPeriodeSlutt = periodeMidt.plusWeeks(1)
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(
                periode = periode,
                sats = sats,
                stengt = listOf(toStengtPeriode(Periode(stengtPeriodeStart, stengtPeriodeSlutt))),
            )
            val deltakere = listOf(
                createDeltaker(
                    id = deltakerId1,
                    periode = periode,
                    deltakelsesmengder = listOf(
                        Deltakelsesmengde(periodeStart, 50.0),
                        Deltakelsesmengde(periodeMidt, 51.0),
                    ),
                ),
                createDeltaker(
                    id = deltakerId2,
                    periode = Periode(periodeStart, periodeMidt),
                    deltakelsesmengder = listOf(Deltakelsesmengde(periodeStart, 50.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                satser = setOf(SatsPeriode(periode, sats)),
                stengt = setOf(StengtPeriode(Periode(stengtPeriodeStart, stengtPeriodeSlutt), "Stengt")),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId1,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 50.0),
                            DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 51.0),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId2,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 50.0),
                        ),
                    ),
                ),
            )
            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId1,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, stengtPeriodeStart),
                                0.11364,
                                sats,
                            ),
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(stengtPeriodeSlutt, periodeSlutt),
                                0.27273,
                                sats,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId2,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(periodeStart, stengtPeriodeStart),
                                0.11364,
                                sats,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("flere stengt hos arrangør perioder i én deltakelses periode") {
            val periode = Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 1))
            val deltakerId = UUID.randomUUID()

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
                    id = deltakerId,
                    periode = periode,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 50,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
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
            val deltakelseId1 = UUID.randomUUID()
            val deltakelseId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 20205)
            val deltakere = listOf(
                createDeltaker(
                    id = deltakelseId1,
                    periode = deltakelse1,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
                createDeltaker(
                    id = deltakelseId2,
                    periode = deltakelse2,
                    deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                // Rundet ned fra 9663.26086...
                belop = 9663,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId1,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(deltakelse1, 0.21739, 20205)),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId2,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(deltakelse2, 0.26087, 20205)),
                    ),
                ),
            )
        }

        test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
            val periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1))

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 20205)

            (2..31).forEach { dayOfMonth ->
                val deltakelsePeriode = Periode(periode.start, periode.start.withDayOfMonth(dayOfMonth))

                val deltakere = listOf(
                    createDeltaker(
                        periode = deltakelsePeriode,
                        deltakelsesmengder = listOf(Deltakelsesmengde(periode.start, 100.0)),
                    ),
                )

                val utbetaling = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

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
    }

    context("periode ulik én måned") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 10)
            val deltakere = listOf(
                createDeltaker(deltakerId1, periode, listOf(Deltakelsesmengde(periode.start, 100.0))),
                createDeltaker(deltakerId2, periode, listOf(Deltakelsesmengde(periode.start, 100.0))),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
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
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 7, 1))

            val heleUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 7, 7), // Mandag
                LocalDate.of(2025, 7, 13), // Søndag
            )

            val hverdagerUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 7, 7), // Mandag
                LocalDate.of(2025, 7, 11), // Fredag
            )

            val helgFraUke36OgHeleUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 7, 5), // Lørdag
                LocalDate.of(2025, 7, 13), // Søndag
            )

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            val deltakerId3 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(deltakerId1, heleUke37, listOf(Deltakelsesmengde(heleUke37.start, 100.0))),
                createDeltaker(deltakerId2, hverdagerUke37, listOf(Deltakelsesmengde(hverdagerUke37.start, 100.0))),
                createDeltaker(
                    deltakerId3,
                    helgFraUke36OgHeleUke37,
                    listOf(Deltakelsesmengde(helgFraUke36OgHeleUke37.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId1,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22581, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId2,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.16129, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId3,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgFraUke36OgHeleUke37, 0.29032, sats)),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker ikke beregnet beløp") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 9, 1))

            val heleUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 9, 8), // Mandag
                LocalDate.of(2025, 9, 14), // Søndag
            )

            val hverdagerUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 9, 8), // Mandag
                LocalDate.of(2025, 9, 12), // Fredag
            )

            val helgFraUke36OgHeleUke37 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 9, 6), // Lørdag
                LocalDate.of(2025, 9, 14), // Søndag
            )

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            val deltakerId3 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = sats)
            val deltakere = listOf(
                createDeltaker(deltakerId1, heleUke37, listOf(Deltakelsesmengde(heleUke37.start, 100.0))),
                createDeltaker(deltakerId2, hverdagerUke37, listOf(Deltakelsesmengde(hverdagerUke37.start, 100.0))),
                createDeltaker(
                    deltakerId3,
                    helgFraUke36OgHeleUke37,
                    listOf(Deltakelsesmengde(helgFraUke36OgHeleUke37.start, 100.0)),
                ),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId1,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(heleUke37, 0.22727, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId2,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(hverdagerUke37, 0.22727, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId3,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(helgFraUke36OgHeleUke37, 0.22727, sats)),
                ),
            )
        }
    }

    context("beregning av månedsverk etter 1. januar 2026") {
        test("50% deltakelse tilsvarer halvt månedsverk") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            val deltakerId3 = UUID.randomUUID()
            val deltakerId4 = UUID.randomUUID()

            val gjennomforing = createGjennomforingForForhandsgodkjentPris(periode = periode, sats = 100)
            val deltakere = listOf(
                createDeltaker(deltakerId1, periode, listOf(Deltakelsesmengde(periode.start, 20.0))),
                createDeltaker(deltakerId2, periode, listOf(Deltakelsesmengde(periode.start, 50.0))),
                createDeltaker(deltakerId3, periode, listOf(Deltakelsesmengde(periode.start, 51.0))),
                createDeltaker(deltakerId4, periode, listOf(Deltakelsesmengde(periode.start, 80.0))),
            )

            val result = FastSatsPerTiltaksplassPerManedBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                satser = setOf(SatsPeriode(periode, sats)),
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakerId1,
                        listOf(DeltakelsesprosentPeriode(periode, 20.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakerId2,
                        listOf(DeltakelsesprosentPeriode(periode, 50.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakerId3,
                        listOf(DeltakelsesprosentPeriode(periode, 51.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakerId4,
                        listOf(DeltakelsesprosentPeriode(periode, 80.0)),
                    ),
                ),
            )
            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId1,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 0.5, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId2,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 0.5, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId3,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakerId4,
                    setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 1.0, sats)),
                ),
            )
        }
    }
})
