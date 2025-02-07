package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningAftTest : FunSpec({

    context("AFT utbetaling beregning") {
        val periodeStart = LocalDate.of(2023, 6, 1)
        val periodeMidt = LocalDate.of(2023, 6, 16)
        val periodeSlutt = LocalDate.of(2023, 7, 1)

        test("beløp beregnes fra månedsverk til deltakere og sats") {
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()
            forAll(
                row(
                    setOf(
                        DeltakelsePerioder(
                            deltakelseId = deltakerId1,
                            perioder = listOf(
                                DeltakelsePeriode(periodeStart, periodeSlutt, 100.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
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
                                DeltakelsePeriode(periodeStart, periodeSlutt, 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
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
                                DeltakelsePeriode(periodeStart, periodeMidt, 40.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
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
                                DeltakelsePeriode(periodeStart, periodeMidt, 49.0),
                                DeltakelsePeriode(periodeMidt, periodeSlutt, 50.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
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
                                DeltakelsePeriode(periodeStart, periodeSlutt, 100.0),
                            ),
                        ),
                        DeltakelsePerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsePeriode(periodeStart, periodeSlutt, 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
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
                                DeltakelsePeriode(periodeStart, periodeMidt, 49.0),
                                DeltakelsePeriode(periodeMidt, periodeSlutt, 50.0),
                            ),
                        ),
                        DeltakelsePerioder(
                            deltakelseId = deltakerId2,
                            perioder = listOf(
                                DeltakelsePeriode(periodeStart, periodeMidt, 49.0),
                            ),
                        ),
                    ),
                    UtbetalingBeregningAft.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                            DeltakelseManedsverk(deltakerId2, 0.25),
                        ),
                    ),
                ),
            ) { deltakelser, expectedBeregning ->
                val periode = Periode(periodeStart, periodeSlutt)
                val input = UtbetalingBeregningAft.Input(periode, 100, deltakelser)

                val beregning = UtbetalingBeregningAft.beregn(input)

                beregning.output shouldBe expectedBeregning
            }
        }
    }
})
