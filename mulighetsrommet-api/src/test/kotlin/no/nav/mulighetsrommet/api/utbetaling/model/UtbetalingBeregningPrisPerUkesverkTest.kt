package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningPrisPerUkesverkTest : FunSpec({
    context("beregning for pris per ukesverk") {
        test("beløp beregnes fra ukesverk til deltakere og sats") {
            val periodeStart = LocalDate.of(2023, 6, 1)
            val periodeMidt = LocalDate.of(2023, 6, 8)
            val periodeSlutt = LocalDate.of(2023, 6, 15)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    setOf(
                        DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeSlutt)),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 100,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeSlutt),
                                        2.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePeriode(deltakerId1, Periode(periodeStart, LocalDate.of(2023, 6, 5))),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 20,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, LocalDate.of(2023, 6, 5)),
                                        0.4,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    setOf(
                        DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeSlutt)),
                        DeltakelsePeriode(deltakerId2, Periode(periodeStart, periodeMidt)),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 150,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeSlutt),
                                        2.0,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeMidt),
                                        1.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { deltakelser, expectedBeregning ->
                val satser = setOf(SatsPeriode(Periode(periodeStart, periodeSlutt), 50))
                val input = UtbetalingBeregningPrisPerUkesverk.Input(satser, setOf(), deltakelser)

                val beregning = UtbetalingBeregningPrisPerUkesverk.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }

        test("perioder med stengt hos arrangør overstyrer ukesverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 8)
            val periodeSlutt = LocalDate.of(2025, 2, 15)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                    setOf(
                        DeltakelsePeriode(
                            deltakelseId = deltakerId1,
                            periode = Periode(periodeStart, periodeSlutt),
                        ),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 50,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeMidt, periodeSlutt),
                                        1.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    setOf(
                        StengtPeriode(Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)), "Stengt 1"),
                        StengtPeriode(Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 11)), "Stengt 2"),
                    ),
                    setOf(
                        DeltakelsePeriode(
                            deltakelseId = deltakerId1,
                            periode = Periode(periodeStart, periodeSlutt),
                        ),
                        DeltakelsePeriode(
                            deltakelseId = deltakerId2,
                            periode = Periode(periodeMidt, periodeSlutt),
                        ),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 110,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(LocalDate.of(2025, 2, 5), LocalDate.of(2025, 2, 10)),
                                        0.6,
                                    ),
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(LocalDate.of(2025, 2, 11), LocalDate.of(2025, 2, 15)),
                                        0.8,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(LocalDate.of(2025, 2, 11), LocalDate.of(2025, 2, 15)),
                                        0.8,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { stengt, deltakelser, expectedBeregning ->
                val satser = setOf(SatsPeriode(Periode(periodeStart, periodeSlutt), 50))
                val input = UtbetalingBeregningPrisPerUkesverk.Input(satser, stengt, deltakelser)

                val beregning = UtbetalingBeregningPrisPerUkesverk.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }
    }

    context("periode ulik én uke") {
        test("en deltakere over 6 uker") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 12))
            periode.getDurationInDays() shouldBe 6 * 7

            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 10)),
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, periode),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerUkesverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 60,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId1,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 6.0),
                        ),
                    ),
                ),
            )
        }
    }
})
