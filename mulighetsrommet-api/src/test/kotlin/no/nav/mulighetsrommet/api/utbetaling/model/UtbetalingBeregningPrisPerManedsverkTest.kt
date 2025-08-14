package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.*

class UtbetalingBeregningPrisPerManedsverkTest : FunSpec({
    context("enkel beregning full og halv") {
        test("én deltaker full periode") {
            val periode = Periode(LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1))
            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode,
                100,
                setOf(),
                setOf(DeltakelsePeriode(deltakelseId = deltakerId1, periode = periode)),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)

            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 100,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 1.0),
                ),
            )
        }

        test("én deltaker halv periode") {
            val periode = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1))
            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode,
                700_000,
                setOf(),
                setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltakerId1,
                        periode = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 350_000,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                ),
            )
        }

        test("to deltakere med halve perioder") {
            val periode = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode,
                202,
                setOf(),
                setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltakerId1,
                        periode = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltakerId2,
                        periode = Periode(LocalDate.of(2026, 4, 16), LocalDate.of(2026, 5, 1)),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 202,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                    DeltakelseManedsverk(deltakerId2, 0.5),
                ),
            )
        }
    }
    context("stengt perioder") {
        test("én deltaker full periode, stengt start til midt") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)

            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                Periode(periodeStart, periodeSlutt),
                100,
                setOf(StengtPeriode(Periode(periodeStart, periodeMidt), "Stengt")),
                setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltakerId1,
                        periode = Periode(periodeStart, periodeSlutt),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                ),
            )
        }

        test("én deltaker full periode, stengt halve") {
            val periodeStart = LocalDate.of(2025, 2, 1)
            val periodeMidt = LocalDate.of(2025, 2, 15)
            val periodeSlutt = LocalDate.of(2025, 3, 1)

            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                Periode(periodeStart, periodeSlutt),
                100,
                setOf(StengtPeriode(Periode(periodeStart.plusWeeks(1), periodeMidt.plusWeeks(1)), "Stengt")),
                setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltakerId1,
                        periode = Periode(periodeStart, periodeSlutt),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
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
                DeltakelsePeriode(
                    deltakelseId = deltakerId1,
                    periode = Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1)),
                ),
            )
            val periode = Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1))
            val input = UtbetalingBeregningPrisPerManedsverk.Input(periode, 100, stengt, deltakelser)

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)

            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5),
                ),
            )
        }
    }

    test("utbetaling og tilsagn er likt for forskjellige perioder av en måned") {
        (2..31).forEach {
            val periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, it))

            val utbetaling = UtbetalingBeregningPrisPerManedsverk.beregn(
                UtbetalingBeregningPrisPerManedsverk.Input(
                    periode = Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1)),
                    20205,
                    emptySet(),
                    setOf(
                        DeltakelsePeriode(
                            deltakelseId = UUID.randomUUID(),
                            periode = periode,
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

    context("periode ulik én måned") {
        test("to deltakere over 2 måneder") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode,
                10,
                setOf(),
                setOf(
                    DeltakelsePeriode(deltakelseId = deltakerId1, periode = periode),
                    DeltakelsePeriode(deltakelseId = deltakerId2, periode = periode),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 2.0),
                    DeltakelseManedsverk(deltakerId2, 2.0),
                ),
            )
        }

        test("periode 4 dager, stengt dag én og tre gir 2/31") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode,
                31,
                setOf( // Stengt dag én og tre
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2)), "1"),
                    StengtPeriode(Periode(LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 4)), "2"),
                ),
                setOf(
                    DeltakelsePeriode(deltakelseId = deltakerId1, periode = periode),
                    DeltakelsePeriode(deltakelseId = deltakerId2, periode = periode),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output.belop shouldBe 4 // 2 * 2/31 * 31
        }

        test("én dag i februar er verdt mer enn én dag i januar") {
            val periodeJanuar = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))
            val periodeFebruar = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 2))
            val deltakerId1 = UUID.randomUUID()

            val beregningJanuar = UtbetalingBeregningPrisPerManedsverk.beregn(
                UtbetalingBeregningPrisPerManedsverk.Input(
                    periodeJanuar,
                    31_000,
                    setOf(),
                    setOf(DeltakelsePeriode(deltakerId1, periodeJanuar)),
                ),
            )
            val beregningFebruar = UtbetalingBeregningPrisPerManedsverk.beregn(
                UtbetalingBeregningPrisPerManedsverk.Input(
                    periodeFebruar,
                    31_000,
                    setOf(),
                    setOf(DeltakelsePeriode(deltakerId1, periodeFebruar)),
                ),
            )
            beregningJanuar.output.belop shouldBe 1000
            beregningFebruar.output.belop shouldBe 1107
        }
    }
})
