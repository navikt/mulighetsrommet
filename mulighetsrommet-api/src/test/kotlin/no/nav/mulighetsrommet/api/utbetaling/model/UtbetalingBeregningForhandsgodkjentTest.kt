package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningForhandsgodkjent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningForhandsgodkjentTest : FunSpec({
    context("forhåndsgodkjent utbetaling beregning") {
        test("beløp beregnes fra månedsverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 16)
            val periodeSlutt = LocalDate.of(2023, 7, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 40.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 25,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.25),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsePeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 75,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                        DeltakelsePerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 150,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 1.0),
                            DeltakelseManedsverk(deltakerId2, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsePeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                        DeltakelsePerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                            DeltakelseManedsverk(deltakerId2, 0.25),
                        ),
                    ),
                ),
            ) { deltakelser, expectedBeregning ->
                val periode = Periode(periodeStart, periodeSlutt)
                val input = UtbetalingBeregningForhandsgodkjent.Input(periode, 100, setOf(), deltakelser)

                val beregning = UtbetalingBeregningForhandsgodkjent.beregn(input)

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
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeSlutt), 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.5),
                        ),
                    ),
                ),
                row(
                    setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                                DeltakelsePeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                            ),
                        ),
                        DeltakelsePerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningForhandsgodkjent.Output(
                        belop = 50,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.375),
                            DeltakelseManedsverk(deltakerId2, 0.125),
                        ),
                    ),
                ),
            ) { stengt, deltakelser, expectedBeregning ->
                val periode = Periode(periodeStart, periodeSlutt)
                val input = UtbetalingBeregningForhandsgodkjent.Input(periode, 100, stengt, deltakelser)

                val beregning = UtbetalingBeregningForhandsgodkjent.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }

        test("månedsverk blir beregnet med tilstrekkelig presisjon") {
            val periodeStart = LocalDate.of(2025, 6, 1)
            val periodeMidt = LocalDate.of(2025, 6, 16)
            val periodeSlutt = LocalDate.of(2025, 7, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode(periodeStart, periodeSlutt),
                sats = 100,
                stengt = setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltakerId1,
                        perioder = listOf(
                            DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                            DeltakelsePeriode(Periode(periodeMidt, periodeSlutt), 50.0),
                        ),
                    ),
                    DeltakelsePerioder(
                        deltakelseId = deltakerId2,
                        perioder = listOf(
                            DeltakelsePeriode(Periode(periodeStart, periodeMidt), 49.0),
                        ),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningForhandsgodkjent.beregn(input)

            beregning.output shouldBe UtbetalingBeregningForhandsgodkjent.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.38333),
                    DeltakelseManedsverk(deltakerId2, 0.11667),
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
                DeltakelsePerioder(
                    deltakelseId = deltakerId1,
                    perioder = listOf(
                        DeltakelsePeriode(Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1)), 100.0),
                    ),
                ),
            )
            val periode = Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1))
            val input = UtbetalingBeregningForhandsgodkjent.Input(periode, 100, stengt, deltakelser)

            val beregning = UtbetalingBeregningForhandsgodkjent.beregn(input)

            beregning.output shouldBe UtbetalingBeregningForhandsgodkjent.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                ),
            )
        }

        test("rundes opp til slutt") {
            // 5/31 * 20205 = 3258.87
            UtbetalingBeregningForhandsgodkjent.beregn(
                UtbetalingBeregningForhandsgodkjent.Input(
                    periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1)),
                    20205,
                    emptySet(),
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = UUID.randomUUID(),
                            perioder = listOf(DeltakelsePeriode(Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 6)), 100.0)),
                        ),
                    ),
                ),
            ).output.belop shouldBe 3259
        }

        test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
            (2..31).forEach {
                val periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, it))

                val utbetaling = UtbetalingBeregningForhandsgodkjent.beregn(
                    UtbetalingBeregningForhandsgodkjent.Input(
                        periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1)),
                        20205,
                        emptySet(),
                        setOf(
                            DeltakelsePerioder(
                                deltakelseId = UUID.randomUUID(),
                                perioder = listOf(DeltakelsePeriode(periode, 100.0)),
                            ),
                        ),
                    ),
                )

                val tilsagn = TilsagnBeregningForhandsgodkjent.beregn(
                    TilsagnBeregningForhandsgodkjent.Input(
                        periode = periode,
                        sats = 20205,
                        antallPlasser = 1,
                    ),
                )

                utbetaling.output.belop shouldBe tilsagn.output.belop
            }
        }
    }
})
