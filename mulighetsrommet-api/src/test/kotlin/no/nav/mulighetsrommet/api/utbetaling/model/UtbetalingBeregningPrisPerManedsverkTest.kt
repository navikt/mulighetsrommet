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
                    DeltakelseManedsverk(deltakerId1, 1.0, periode),
                ),
            )
        }

        test("én deltaker halv periode") {
            val periodeStart = LocalDate.of(2026, 4, 1)
            val periodeMidt = LocalDate.of(2026, 4, 16)
            val periodeSlutt = LocalDate.of(2026, 5, 1)

            val deltakerId1 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                Periode(periodeStart, periodeSlutt),
                700_000,
                setOf(),
                setOf(
                    DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeMidt)),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 350_000,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5, Periode(periodeStart, periodeMidt)),
                ),
            )
        }

        test("to deltakere med halve perioder") {
            val periodeStart = LocalDate.of(2026, 4, 1)
            val periodeMidt = LocalDate.of(2026, 4, 16)
            val periodeSlutt = LocalDate.of(2026, 5, 1)

            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                Periode(periodeStart, periodeSlutt),
                202,
                setOf(),
                setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltakerId1,
                        periode = Periode(periodeStart, periodeMidt),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltakerId2,
                        periode = Periode(periodeMidt, periodeSlutt),
                    ),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 202,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5, Periode(periodeStart, periodeMidt)),
                    DeltakelseManedsverk(deltakerId2, 0.5, Periode(periodeMidt, periodeSlutt)),
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
                    DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeSlutt)),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.5, Periode(periodeMidt, periodeSlutt)),
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
                    DeltakelsePeriode(deltakerId1, Periode(periodeStart, periodeSlutt)),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 0.25, Periode(periodeStart, periodeStart.plusWeeks(1))),
                    DeltakelseManedsverk(deltakerId1, 0.25, Periode(periodeMidt.plusWeeks(1), periodeSlutt)),
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
            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1)),
                100,
                stengt,
                setOf(
                    DeltakelsePeriode(deltakerId1, Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1))),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)

            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 50,
                deltakelser = setOf(
                    DeltakelseManedsverk(
                        deltakerId1,
                        0.1,
                        Periode(LocalDate.of(2023, 4, 2), LocalDate.of(2023, 4, 5)),
                    ),
                    DeltakelseManedsverk(
                        deltakerId1,
                        0.4,
                        Periode(LocalDate.of(2023, 4, 19), LocalDate.of(2023, 5, 1)),
                    ),
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

    context("beregning av månedsverk før 1. august 2025") {
        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = periode,
                sats = 10,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, periode),
                    DeltakelsePeriode(deltakerId2, periode),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 2.0, periode),
                    DeltakelseManedsverk(deltakerId2, 2.0, periode),
                ),
            )
        }

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

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = periode,
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
                DeltakelseManedsverk(deltakerId1, 0.22581, heleUke37),
                DeltakelseManedsverk(deltakerId2, 0.16129, hverdagerUke37),
                DeltakelseManedsverk(deltakerId3, 0.29032, helgFraUke36OgHeleUke37),
            )
        }
    }

    context("beregning av månedsverk etter 1. august 2025") {

        test("to deltakere over 2 måneder gir fire månedsverk") {
            val periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
            val deltakerId1 = UUID.randomUUID()
            val deltakerId2 = UUID.randomUUID()

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = periode,
                sats = 10,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePeriode(deltakerId1, periode),
                    DeltakelsePeriode(deltakerId2, periode),
                ),
            )

            val beregning = UtbetalingBeregningPrisPerManedsverk.beregn(input)
            beregning.output shouldBe UtbetalingBeregningPrisPerManedsverk.Output(
                belop = 40,
                deltakelser = setOf(
                    DeltakelseManedsverk(deltakerId1, 2.0, periode),
                    DeltakelseManedsverk(deltakerId2, 2.0, periode),
                ),
            )
        }

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

            val input = UtbetalingBeregningPrisPerManedsverk.Input(
                periode = periode,
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
                DeltakelseManedsverk(deltakerId1, 0.22727, heleUke37),
                DeltakelseManedsverk(deltakerId2, 0.22727, hverdagerUke37),
                DeltakelseManedsverk(deltakerId3, 0.22727, helgFraUke36OgHeleUke37),
            )
        }
    }
})
