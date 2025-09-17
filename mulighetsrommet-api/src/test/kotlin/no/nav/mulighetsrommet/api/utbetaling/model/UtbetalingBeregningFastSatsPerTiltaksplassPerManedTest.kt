package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningFastSatsPerTiltaksplassPerManedTest : FunSpec({
    context("beregning for fast sats per tiltaksplass per måned") {
        test("beløp beregnes fra månedsverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 16)
            val periodeSlutt = LocalDate.of(2023, 7, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 40.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 25,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.25),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 75,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 150,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                            DeltakelseManedsverk(deltakerId2, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                            DeltakelseManedsverk(deltakerId2, 0.25),
                        ),
                    ),
                ),
            ) { deltakelser, expectedBeregning ->
                val periode = Periode(periodeStart, periodeSlutt)
                val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(periode, 100, setOf(), deltakelser)

                val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }

        test("perioder med stengt hos arrangør overstyrer månedsverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                    setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.375),
                            DeltakelseManedsverk(deltakerId2, 0.125),
                        ),
                    ),
                ),
            ) { stengt, deltakelser, expectedBeregning ->
                val periode = Periode(periodeStart, periodeSlutt)
                val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(periode, 100, stengt, deltakelser)

                val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }

        test("månedsverk blir beregnet med tilstrekkelig presisjon") {
            val periodeStart = LocalDate.of(2026, 1, 1)
            val periodeMidt = LocalDate.of(2026, 1, 16)
            val periodeSlutt = LocalDate.of(2026, 2, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode = Periode(periodeStart, periodeSlutt),
                sats = 100,
                stengt = setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId1,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                            DeltakelsesprosentPeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId2,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(Periode(periodeStart, periodeMidt), 49.0),
                        ),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)

            beregning.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 50,
                deltakelser = setOf(
                    // 5 / 22 * 0.5 + 6 / 22 * 1
                    DeltakelseManedsverk(deltakerId1, 0.38636),
                    // 6 / 22 * 0.5
                    DeltakelseManedsverk(deltakerId2, 0.11364),
                ),
            )
        }

        test("flere stengt hos arrangør perioder i én deltakelses periode") {
            val deltakerId1 = UUID.randomUUID()

            // 1 pluss 14 = 15 dager stengt = 50 %
            val stengt = setOf(
                StengtPeriode(Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 2)), "Stengt"),
                StengtPeriode(Periode(LocalDate.of(2023, 4, 5), LocalDate.of(2023, 4, 19)), "Stengt"),
            )
            val deltakelser = setOf(
                DeltakelseDeltakelsesprosentPerioder(
                    deltakelseId = deltakerId1,
                    perioder = listOf(
                        DeltakelsesprosentPeriode(Periode.forMonthOf(LocalDate.of(2023, 4, 1)), 100.0),
                    ),
                ),
            )
            val periode = Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1))
            val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(periode, 100, stengt, deltakelser)

            val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)

            beregning.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                ),
            )
        }

        test("beregnet beløp rundes av til nærmeste hele krone") {
            val deltakelse1 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 12, 1), // Mandag 1. desember
                LocalDate.of(2025, 12, 7), // Fredag 7. desember
            )
            val deltakelse2 = Periode.fromInclusiveDates(
                LocalDate.of(2025, 12, 1), // Mandag 1. desember
                LocalDate.of(2025, 12, 8), // Mandag 8. desember
            )
            val deltakelseId1 = UUID.randomUUID()
            val deltakelseId2 = UUID.randomUUID()

            val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 12, 1)),
                sats = 20205,
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakelseId1,
                        perioder = listOf(DeltakelsesprosentPeriode(deltakelse1, 100.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakelseId2,
                        perioder = listOf(DeltakelsesprosentPeriode(deltakelse2, 100.0)),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)

            beregning.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                // Rundet ned fra 9663.26086...
                belop = 9663,
                deltakelser = setOf(
                    // 5 / 23 * 20205 = 4392.39130...
                    DeltakelseManedsverk(deltakelseId1, 0.21739),
                    // 6 / 23 *  20205 = 5270.8695...
                    DeltakelseManedsverk(deltakelseId2, 0.26087),
                ),
            )
        }

        test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
            (2..31).forEach {
                val periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, it))

                val utbetaling = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(
                    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 3, 1)),
                        20205,
                        emptySet(),
                        setOf(
                            DeltakelseDeltakelsesprosentPerioder(
                                deltakelseId = UUID.randomUUID(),
                                perioder = listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                            ),
                        ),
                    ),
                )

                val tilsagn = TilsagnBeregningPrisPerManedsverk.beregn(
                    TilsagnBeregningPrisPerManedsverk.Input(
                        periode = periode,
                        sats = 20205,
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

            val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode,
                10,
                setOf(),
                setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId1,
                        perioder = listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId2,
                        perioder = listOf(DeltakelsesprosentPeriode(periode, 100.0)),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)
            beregning.output shouldBe UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 40,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 2.0),
                    DeltakelseManedsverk(deltakerId2, 2.0),
                ),
            )
        }
    }

    context("beregning av månedsverk før 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker beregnet beløp") {
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

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 7, 1)),
                sats = 100,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, heleUke37),
                    DeltakelsePeriode(deltakerId2, hverdagerUke37),
                    DeltakelsePeriode(deltakerId3, helgFraUke36OgHeleUke37),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)

            // Hvert beregnet månedsverk tilsvarer 5/22 (5 ukedager av totalt 22 ukedager i september)
            beregning.output.deltakelser shouldBe setOf(
                DeltakelseManedsverk(deltakerId1, 0.22581),
                DeltakelseManedsverk(deltakerId2, 0.16129),
                DeltakelseManedsverk(deltakerId3, 0.29032),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {
        test("helgedager før og etter en periode på fem hverdager påvirker ikke beregnet beløp") {
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

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = Periode.forMonthOf(LocalDate.of(2025, 9, 1)),
                sats = 100,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, heleUke37),
                    DeltakelsePeriode(deltakerId2, hverdagerUke37),
                    DeltakelsePeriode(deltakerId3, helgFraUke36OgHeleUke37),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)

            // Hvert beregnet månedsverk tilsvarer 5/22 (5 ukedager av totalt 22 ukedager i september)
            beregning.output.deltakelser shouldBe setOf(
                DeltakelseManedsverk(deltakerId1, 0.22727),
                DeltakelseManedsverk(deltakerId2, 0.22727),
                DeltakelseManedsverk(deltakerId3, 0.22727),
            )
        }
    }
})
