package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createDeltaker
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.createGjennomforingForPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toAvtaltSats
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers.toStengtPeriode
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class UtbetalingBeregningPrisPerHeleUkesverkTest : FunSpec({

    test("justerer utbetalingsperiode til å gjelde for nærmeste hele uker") {
        val januar = Periode.forMonthOf(LocalDate.of(2025, 1, 1))
        val justertJanuar = Periode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 2, 3))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(januar) shouldBe justertJanuar

        val februar = Periode.forMonthOf(LocalDate.of(2025, 2, 1))
        val justertFebruar = Periode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 3, 3))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(februar) shouldBe justertFebruar

        val mars = Periode.forMonthOf(LocalDate.of(2025, 3, 1))
        val justertMars = Periode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 31))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(mars) shouldBe justertMars

        val september = Periode.forMonthOf(LocalDate.of(2025, 9, 1))
        val justertSeptember = Periode(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 29))
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(september) shouldBe justertSeptember

        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(justertJanuar) shouldBe justertJanuar
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(justertFebruar) shouldBe justertFebruar
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(justertMars) shouldBe justertMars
        PrisPerHeleUkeBeregning.justerPeriodeForBeregning(justertSeptember) shouldBe justertSeptember
    }

    context("filtrering av deltakere og satser") {
        test("deltakere med irrelevant status inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 1, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
            )
            val deltakere = listOf(
                createDeltaker(periode, status = DeltakerStatusType.DELTAR),
                createDeltaker(periode, status = DeltakerStatusType.HAR_SLUTTET),
                createDeltaker(periode, status = DeltakerStatusType.FULLFORT),
                createDeltaker(periode, status = DeltakerStatusType.AVBRUTT),
                createDeltaker(periode, status = DeltakerStatusType.IKKE_AKTUELL),
                createDeltaker(periode, status = DeltakerStatusType.FEILREGISTRERT),
                createDeltaker(periode, status = DeltakerStatusType.PABEGYNT_REGISTRERING),
                createDeltaker(periode, status = DeltakerStatusType.SOKT_INN),
                createDeltaker(periode, status = DeltakerStatusType.VENTER_PA_OPPSTART),
            )

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser.map { it.deltakelseId } shouldBe setOf(
                deltakere[0].id,
                deltakere[1].id,
                deltakere[2].id,
                deltakere[3].id,
            )
        }

        test("satser utenfor utbetalingsperioden inkluderes ikke i beregningen") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(
                    toAvtaltSats(LocalDate.of(2025, 1, 1), 50),
                    toAvtaltSats(LocalDate.of(2026, 2, 15), 100),
                    toAvtaltSats(LocalDate.of(2026, 3, 1), 150),
                ),
            )
            val deltakere = listOf(createDeltaker(periode))

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.satser shouldBe setOf(
                SatsPeriode(Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15)), 50),
                SatsPeriode(Periode(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 1)), 100),
            )
        }

        test("deltakere utenfor utbetalingsperioden inkluderes ikke") {
            val periode = Periode.forMonthOf(LocalDate.of(2026, 2, 1))

            val gjennomforing = createGjennomforingForPrisPerHeleUkesverk(
                periode = periode,
                satser = listOf(toAvtaltSats(periode.start, 100)),
            )
            val deltakere = listOf(
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))),
                createDeltaker(Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))),
            )

            val result = PrisPerHeleUkeBeregning.beregn(gjennomforing, deltakere, periode)

            result.input.deltakelser shouldBe setOf(DeltakelsePeriode(deltakere[0].id, periode))
        }
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
