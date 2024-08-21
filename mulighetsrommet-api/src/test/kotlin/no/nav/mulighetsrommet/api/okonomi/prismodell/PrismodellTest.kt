package no.nav.mulighetsrommet.api.okonomi.prismodell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PrismodellTest : FunSpec({
    context("AFT tilsagn beregning") {
        test("en plass en måned = sats") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 146,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2021, 1, 1),
                periodeSlutt = LocalDate.of(2021, 1, 31),
            ) shouldBe 146
        }
        test("flere plasser en måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 12,
                antallPlasser = 6,
                periodeStart = LocalDate.of(2021, 1, 1),
                periodeSlutt = LocalDate.of(2021, 1, 31),
            ) shouldBe 72
        }
        test("en plass halv måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 88,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2021, 4, 1),
                periodeSlutt = LocalDate.of(2021, 4, 15),
            ) shouldBe 44
        }
        test("flere plasser en og en halv måned") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 10,
                antallPlasser = 10,
                periodeStart = LocalDate.of(2021, 3, 1),
                periodeSlutt = LocalDate.of(2021, 4, 15),
            ) shouldBe 150
        }
        test("ingen plasser") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 10,
                antallPlasser = 0,
                periodeStart = LocalDate.of(2021, 3, 1),
                periodeSlutt = LocalDate.of(2021, 4, 15),
            ) shouldBe 0
        }
        test("skuddår/ikke skuddår") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 1000,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2023, 2, 1),
                periodeSlutt = LocalDate.of(2023, 2, 28),
            ) shouldBe 1000

            Prismodell.AFT.beregnTilsagnBelop(
                sats = 1000,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 2, 1),
                periodeSlutt = LocalDate.of(2024, 2, 28),
            ) shouldBe 966
        }
        test("0 pris = 0 beløp") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 0,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2023, 2, 1),
                periodeSlutt = LocalDate.of(2023, 2, 28),
            ) shouldBe 0
        }
        test("tom periode kaster exception") {
            shouldThrow<IllegalArgumentException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 10,
                    antallPlasser = 7,
                    periodeStart = LocalDate.of(2023, 2, 2),
                    periodeSlutt = LocalDate.of(2022, 2, 2),
                )
            }
        }
        test("en dag") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 10,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2088, 1, 1),
                periodeSlutt = LocalDate.of(2088, 1, 1),
            ) shouldBe 0 // runder ned
        }
        test("overflow kaster exception") {
            // overflow i en delberegning for én måned
            shouldThrow<ArithmeticException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = Int.MAX_VALUE,
                    antallPlasser = 2,
                    periodeStart = LocalDate.of(2021, 1, 1),
                    periodeSlutt = LocalDate.of(2021, 1, 31),
                )
            }

            // overflow på summering av 12 måneder
            shouldThrow<ArithmeticException> {
                Prismodell.AFT.beregnTilsagnBelop(
                    sats = 200_000_000,
                    antallPlasser = 1,
                    periodeStart = LocalDate.of(2021, 1, 1),
                    periodeSlutt = LocalDate.of(2021, 12, 31),
                )
            }
        }
        test("reelt eksempel nr 1") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 20205,
                antallPlasser = 24,
                periodeStart = LocalDate.of(2024, 9, 15),
                periodeSlutt = LocalDate.of(2024, 12, 31),
            ) shouldBe 1713384
        }
        test("Arena oppførsel: 30 dager i 31 dagers måned gir 100 %") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 100,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 8, 1),
                periodeSlutt = LocalDate.of(2024, 8, 30),
            ) shouldBe 100
        }
        test("Arena oppførsel: 10 dager i 31 dagers måned gir 10/30 brøk") {
            Prismodell.AFT.beregnTilsagnBelop(
                sats = 100,
                antallPlasser = 1,
                periodeStart = LocalDate.of(2024, 8, 1),
                periodeSlutt = LocalDate.of(2024, 8, 10),
            ) shouldBe 33
        }
    }
})
