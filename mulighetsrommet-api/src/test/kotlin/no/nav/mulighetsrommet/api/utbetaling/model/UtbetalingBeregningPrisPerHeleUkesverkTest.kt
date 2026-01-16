package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class UtbetalingBeregningPrisPerHeleUkesverkTest : FunSpec({
    test("skifter utbetalingsperiode til å gjelde for nærmeste hele uker") {
        val januar = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(januar) shouldBe Periode(
            LocalDate.of(2024, 12, 30),
            LocalDate.of(2025, 2, 3),
        )

        val februar = Periode.forMonthOf(LocalDate.of(2025, 2, 1))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(februar) shouldBe Periode(
            LocalDate.of(2025, 2, 3),
            LocalDate.of(2025, 3, 3),
        )

        val mars = Periode.forMonthOf(LocalDate.of(2025, 3, 1))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(mars) shouldBe Periode(
            LocalDate.of(2025, 3, 3),
            LocalDate.of(2025, 3, 31),
        )

        val september = Periode.forMonthOf(LocalDate.of(2025, 9, 1))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(september) shouldBe Periode(
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 29),
        )
    }

    context("beregning for pris per hele ukesverk") {
        test("5 uker beregnes i januar 2025") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 50)),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 50)),
                stengt = emptySet(),
                deltakelser = setOf(DeltakelsePeriode(deltakere[0].id, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 250,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 5.0, 50)),
                    ),
                ),
            )
        }

        test("hel uke beregnes selv med kun en dag deltatt, med ulike satser") {
            val periode = Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(
                    toAvtaltSats(LocalDate.of(2024, 1, 1), 10),
                    toAvtaltSats(LocalDate.of(2025, 1, 1), 50),
                ),
            )
            val deltakere = listOf(
                createDeltaker(Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2024, 12, 31))),
                createDeltaker(Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 9))),
                createDeltaker(Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 18))),
                createDeltaker(Periode(LocalDate.of(2025, 1, 24), LocalDate.of(2025, 1, 25))),
                createDeltaker(Periode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1))),
            )

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 210,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2024, 12, 31)),
                                1.0,
                                10,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[1].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 9)),
                                1.0,
                                50,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[2].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 18)),
                                1.0,
                                50,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[3].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 1, 24), LocalDate.of(2025, 1, 25)),
                                1.0,
                                50,
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[4].id,
                        setOf(
                            UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                                Periode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1)),
                                1.0,
                                50,
                            ),
                        ),
                    ),
                ),
            )
        }

        test("stengt 4 av 5 dager i en uke gir fortsatt én uke") {
            val mandag = LocalDate.of(2025, 2, 3)
            val fredag = LocalDate.of(2025, 2, 7)
            val lordag = LocalDate.of(2025, 2, 8)
            val periode = Periode(mandag, lordag)

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(mandag, 10)),
                stengt = listOf(toStengtPeriode(Periode(mandag, fredag))),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 10)),
                stengt = setOf(StengtPeriode(Periode(mandag, fredag), "Stengt")),
                deltakelser = setOf(DeltakelsePeriode(deltakere[0].id, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 10,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakere[0].id,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(Periode(fredag, lordag), 1.0, 10)),
                    ),
                ),
            )
        }
    }
})
