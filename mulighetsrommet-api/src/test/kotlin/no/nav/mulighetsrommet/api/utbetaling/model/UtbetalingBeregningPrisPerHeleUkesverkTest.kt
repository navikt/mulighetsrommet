package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningPrisPerHeleUkesverkTest : FunSpec({
    context("beregning for pris per hele ukesverk") {
        test("5 uker beregnes i januar 2025") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            val deltakerId1 = UUID.randomUUID()

            UtbetalingBeregningPrisPerHeleUkesverk.beregn(
                UtbetalingBeregningPrisPerHeleUkesverk.Input(
                    periode,
                    50,
                    setOf(),
                    setOf(DeltakelsePeriode(deltakelseId = deltakerId1, periode = periode)),
                ),
            ).output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 250,
                deltakelser = setOf(
                    DeltakelseUkesverk(deltakerId1, 5.0),
                ),
            )
        }

        test("Hel uke beregnes selv med kun en dag deltatt") {
            val periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            val deltakerId1 = UUID.randomUUID()

            val ukesverk = UtbetalingBeregningPrisPerHeleUkesverk.beregn(
                UtbetalingBeregningPrisPerHeleUkesverk.Input(
                    periode,
                    50,
                    setOf(),
                    setOf(
                        DeltakelsePeriode(deltakelseId = deltakerId1, periode = Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2024, 12, 31))),
                        DeltakelsePeriode(deltakelseId = UUID.randomUUID(), periode = Periode(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 9))),
                        DeltakelsePeriode(deltakelseId = UUID.randomUUID(), periode = Periode(LocalDate.of(2025, 1, 17), LocalDate.of(2025, 1, 18))),
                        DeltakelsePeriode(deltakelseId = UUID.randomUUID(), periode = Periode(LocalDate.of(2025, 1, 24), LocalDate.of(2025, 1, 25))),
                        DeltakelsePeriode(deltakelseId = UUID.randomUUID(), periode = Periode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1))),
                    ),
                ),
            ).output.deltakelser
            ukesverk.shouldHaveSize(5)
            ukesverk.sumOf { it.ukesverk } shouldBe 5.0
        }

        test("stengt halve måneden gir 2 uker") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)

            val deltakerId1 = UUID.randomUUID()

            val periode = Periode(periodeStart, periodeSlutt)

            UtbetalingBeregningPrisPerHeleUkesverk.beregn(
                UtbetalingBeregningPrisPerHeleUkesverk.Input(
                    periode,
                    10,
                    setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                    setOf(
                        DeltakelsePeriode(
                            deltakelseId = deltakerId1,
                            periode = Periode(periodeStart, periodeSlutt),
                        ),
                    ),
                ),
            ).output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 20,
                deltakelser = setOf(
                    DeltakelseUkesverk(deltakerId1, 2.0),
                ),
            )
        }

        test("stengt 4 av 5 dager i en uke gir fortsatt én uke") {
            val mandag = LocalDate.of(2025, 2, 3)
            val lordag = LocalDate.of(2025, 2, 8)

            val deltakerId1 = UUID.randomUUID()

            val periode = Periode(mandag, lordag)

            UtbetalingBeregningPrisPerHeleUkesverk.beregn(
                UtbetalingBeregningPrisPerHeleUkesverk.Input(
                    periode,
                    10,
                    setOf(StengtPeriode(Periode(mandag, lordag.minusDays(1)), "Stengt")),
                    setOf(
                        DeltakelsePeriode(
                            deltakelseId = deltakerId1,
                            periode = periode,
                        ),
                    ),
                ),
            ).output shouldBe UtbetalingBeregningPrisPerHeleUkesverk.Output(
                belop = 10,
                deltakelser = setOf(
                    DeltakelseUkesverk(deltakerId1, 1.0),
                ),
            )
        }
    }
})
