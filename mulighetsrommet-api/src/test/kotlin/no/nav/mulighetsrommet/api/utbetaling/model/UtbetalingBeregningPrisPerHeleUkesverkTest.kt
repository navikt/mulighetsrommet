package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

class UtbetalingBeregningPrisPerHeleUkesverkTest : FunSpec({
    context("beregning for pris per hele ukesverk") {
        test("5 uker beregnes i januar 2025") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 50)),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerHeleUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.input shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Input(
                satser = setOf(SatsPeriode(periode, 50)),
                stengt = setOf(),
                deltakelser = setOf(DeltakelsePeriode(deltakerId, periode)),
            )
            result.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 250,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, 5.0, 50)),
                    ),
                ),
            )
        }

        test("Hel uke beregnes selv med kun en dag deltatt") {
            val deltakelseId1 = UUID.randomUUID()
            val deltakelseId2 = UUID.randomUUID()
            val deltakelseId3 = UUID.randomUUID()
            val deltakelseId4 = UUID.randomUUID()
            val deltakelseId5 = UUID.randomUUID()

            val periode = Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(
                    toAvtaltSats(LocalDate.of(2024, 1, 1), 10),
                    toAvtaltSats(LocalDate.of(2025, 1, 1), 50),
                ),
            )
            val deltakere = listOf(
                createDeltaker(deltakelseId1, Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2024, 12, 31))),
                createDeltaker(deltakelseId2, Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 9))),
                createDeltaker(deltakelseId3, Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 18))),
                createDeltaker(deltakelseId4, Periode(LocalDate.of(2025, 1, 24), LocalDate.of(2025, 1, 25))),
                createDeltaker(deltakelseId5, Periode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1))),
            )

            val result = PrisPerHeleUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.output.deltakelser shouldBe setOf(
                UtbetalingBeregningOutputDeltakelse(
                    deltakelseId1,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2024, 12, 31)),
                            1.0,
                            10,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakelseId2,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 9)),
                            1.0,
                            50,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakelseId3,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 18)),
                            1.0,
                            50,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakelseId4,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            Periode(LocalDate.of(2025, 1, 24), LocalDate.of(2025, 1, 25)),
                            1.0,
                            50,
                        ),
                    ),
                ),
                UtbetalingBeregningOutputDeltakelse(
                    deltakelseId5,
                    setOf(
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                            Periode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1)),
                            1.0,
                            50,
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
            val deltakerId = UUID.randomUUID()

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(mandag, 10)),
                stengt = listOf(toStengtPeriode(Periode(mandag, fredag))),
            )
            val deltakere = listOf(createDeltaker(deltakerId, periode))

            val result = PrisPerHeleUkeBeregning.calculate(gjennomforing, deltakere, periode)

            result.output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 10,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakerId,
                        setOf(UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(Periode(fredag, lordag), 1.0, 10)),
                    ),
                ),
            )
        }
    }
})
