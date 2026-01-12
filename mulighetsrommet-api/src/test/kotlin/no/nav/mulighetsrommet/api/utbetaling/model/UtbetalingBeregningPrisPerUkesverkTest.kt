package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

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

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    listOf(createDeltaker(deltakerId1, periode)),
                    UtbetalingBeregningPrisPerUkesverk.Input(
                        satser = setOf(SatsPeriode(periode, 50)),
                        stengt = setOf(),
                        deltakelser = setOf(DeltakelsePeriode(deltakerId1, periode)),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 100,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        2.0,
                                        50,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(createDeltaker(deltakerId1, Periode(periodeStart, LocalDate.of(2023, 6, 5)))),
                    UtbetalingBeregningPrisPerUkesverk.Input(
                        satser = setOf(SatsPeriode(periode, 50)),
                        stengt = setOf(),
                        deltakelser = setOf(
                            DeltakelsePeriode(deltakerId1, Periode(periodeStart, LocalDate.of(2023, 6, 5))),
                        ),
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
                                        50,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        createDeltaker(deltakerId1, periode),
                        createDeltaker(deltakerId2, Periode(periodeStart, periodeMidt)),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Input(
                        satser = setOf(SatsPeriode(periode, 50)),
                        stengt = setOf(),
                        deltakelser = setOf(
                            DeltakelsePeriode(deltakerId1, periode),
                            DeltakelsePeriode(deltakerId2, Periode(periodeStart, periodeMidt)),
                        ),
                    ),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 150,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        periode,
                                        2.0,
                                        50,
                                    ),
                                ),
                            ),
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeStart, periodeMidt),
                                        1.0,
                                        50,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { deltakere, expectedInput, expectedOutput ->

                val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

                result.input shouldBe expectedInput
                result.output shouldBe expectedOutput
            }
        }

        test("perioder med stengt hos arrangør overstyrer ukesverket til deltakere") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 8)
            val periodeSlutt = LocalDate.of(2025, 2, 15)
            val periode = Periode(periodeStart, periodeSlutt)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            forAll(
                row(
                    listOf(toStengtPeriode(Periode(periodeStart, periodeMidt))),
                    listOf(createDeltaker(deltakerId1, periode)),
                    UtbetalingBeregningPrisPerUkesverk.Output(
                        belop = 50,
                        deltakelser = setOf(
                            UtbetalingBeregningOutputDeltakelse(
                                deltakerId1,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(periodeMidt, periodeSlutt),
                                        1.0,
                                        50,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                row(
                    listOf(
                        toStengtPeriode(Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)), "Stengt 1"),
                        toStengtPeriode(Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 11)), "Stengt 2"),
                    ),
                    listOf(
                        createDeltaker(deltakerId1, periode),
                        createDeltaker(deltakerId2, Periode(periodeMidt, periodeSlutt)),
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
                                deltakerId2,
                                setOf(
                                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                        Periode(LocalDate.of(2025, 2, 11), LocalDate.of(2025, 2, 15)),
                                        0.8,
                                        50,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ) { stengt, deltakere, expectedOutput ->
                val gjennomforing = createGjennomforingForPrisPerUkesverk(
                    periode = periode,
                    satser = listOf(toAvtaltSats(periodeStart, 50)),
                    stengt = stengt,
                )

                val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

                result.output shouldBe expectedOutput
            }
        }
    }

    context("periode ulik én uke") {
        test("en deltakere over 6 uker") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 12))
            periode.getDurationInDays() shouldBe 6 * 7
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 10)),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 10)),
                stengt = setOf(),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerUkesverk.Output(
                belop = 60,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 6.0, 10)),
                    ),
                ),
            )
        }
    }
})
