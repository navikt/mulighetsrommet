package no.nav.mulighetsrommet.api.okonomi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.refusjon.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.refusjon.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.refusjon.model.DeltakelsePerioder
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class PrismodellTest : FunSpec({
    context("AFT tilsagn beregning") {
        test("en plass en måned = sats") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 1, 1),
                periodeSlutt = LocalDate.of(2024, 1, 31),
            ) shouldBe 20205
        }
        test("flere plasser en måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 6,
                periodeStart = LocalDate.of(2024, 1, 1),
                periodeSlutt = LocalDate.of(2024, 1, 31),
            ) shouldBe 121230
        }
        test("en plass halv måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 19500,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2023, 4, 1),
                periodeSlutt = LocalDate.of(2023, 4, 15),
            ) shouldBe 9750
        }
        test("flere plasser en og en halv måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 10,
                periodeStart = LocalDate.of(2024, 3, 1),
                periodeSlutt = LocalDate.of(2024, 4, 15),
            ) shouldBe 303075
        }
        test("ingen plasser") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 0,
                periodeStart = LocalDate.of(2024, 3, 1),
                periodeSlutt = LocalDate.of(2024, 4, 15),
            ) shouldBe 0
        }
        test("skuddår/ikke skuddår") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 19500,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2023, 2, 1),
                periodeSlutt = LocalDate.of(2023, 2, 28),
            ) shouldBe 19500

            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 2, 1),
                periodeSlutt = LocalDate.of(2024, 2, 28),
            ) shouldBe 19599
        }
        test("feil sats kaster exception") {
            shouldThrow<IllegalArgumentException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 15,
                    antallPlasser = 1,
                    periodeStart = LocalDate.of(2023, 2, 1),
                    periodeSlutt = LocalDate.of(2023, 2, 28),
                )
            }
        }
        test("tom periode kaster exception") {
            shouldThrow<IllegalArgumentException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 19500,
                    antallPlasser = 7,
                    periodeStart = LocalDate.of(2023, 2, 2),
                    periodeSlutt = LocalDate.of(2022, 2, 2),
                )
            }
        }
        test("én dag") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 1, 1),
                periodeSlutt = LocalDate.of(2024, 1, 1),
            ) shouldBe 606
        }
        test("overflow kaster exception") {
            // overflow i en delberegning for én måned
            shouldThrow<ArithmeticException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 20205,
                    antallPlasser = Int.MAX_VALUE,
                    periodeStart = LocalDate.of(2024, 1, 1),
                    periodeSlutt = LocalDate.of(2024, 1, 31),
                )
            }

            // overflow på summering av 12 måneder
            shouldThrow<ArithmeticException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 20205,
                    antallPlasser = 9500,
                    periodeStart = LocalDate.of(2024, 1, 1),
                    periodeSlutt = LocalDate.of(2024, 12, 31),
                )
            }
        }
        test("reelt eksempel nr 1") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 24,
                periodeStart = LocalDate.of(2024, 9, 15),
                periodeSlutt = LocalDate.of(2024, 12, 31),
            ) shouldBe 1711768
        }
    }

    context("AFT refusjon beregning") {
        val periodeStart = LocalDateTime.of(2023, 6, 1, 0, 0, 0)
        val periodeMidt = LocalDateTime.of(2023, 6, 16, 0, 0, 0)
        val periodeSlutt = LocalDateTime.of(2023, 7, 1, 0, 0, 0)

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
                    RefusjonKravBeregningAft.Output(
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
                    RefusjonKravBeregningAft.Output(
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
                    RefusjonKravBeregningAft.Output(
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
                    RefusjonKravBeregningAft.Output(
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
                    RefusjonKravBeregningAft.Output(
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
                    RefusjonKravBeregningAft.Output(
                        belop = 100,
                        deltakelser = setOf(
                            DeltakelseManedsverk(deltakerId1, 0.75),
                            DeltakelseManedsverk(deltakerId2, 0.25),
                        ),
                    ),
                ),
            ) { deltakelser, expectedBeregning ->
                val input = RefusjonKravBeregningAft.Input(periodeStart, periodeSlutt, 100, deltakelser)

                val beregning = Prismodell.AFT.beregnRefusjonBelop(input)

                beregning shouldBe expectedBeregning
            }
        }
    }
})
