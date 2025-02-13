package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PeriodeTest : FunSpec({
    test("should create period for month") {
        val period = Periode.forMonthOf(LocalDate.of(2021, 1, 1))

        period shouldBe Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))
    }

    test("should create period from inclusive dates") {
        val period = Periode.fromInclusiveDates(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        period shouldBe Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))
    }

    test("should get duration in days") {
        val period = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        period.getDurationInDays() shouldBe 30
    }

    test("should get last date") {
        val period = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        period.getLastDate() shouldBe LocalDate.of(2021, 1, 30)
    }

    test("should check if date is in period") {
        val period = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        (LocalDate.of(2021, 1, 1) in period) shouldBe true
        (LocalDate.of(2021, 1, 15) in period) shouldBe true
        (LocalDate.of(2021, 1, 31) in period) shouldBe false
    }

    test("should split by month") {
        val period = Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 3, 15))

        period.splitByMonth() shouldBe listOf(
            Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 2, 1)),
            Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 3, 1)),
            Periode(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 15)),
        )
    }

    test("should compare periods") {
        val period1 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val period2 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        period1.compareTo(period2) shouldBe 0

        val period3 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 30))
        period1.compareTo(period3) shouldBe 1
        period3.compareTo(period1) shouldBe -1

        val period4 = Periode(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 1, 31))
        period1.compareTo(period4) shouldBe -1
        period4.compareTo(period1) shouldBe 1
    }

    test("should intersect periods") {
        val period1 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val period2 = Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 2, 15))

        period1.intersect(period2) shouldBe Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 31))

        val period3 = Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 15))
        period1.intersect(period3) shouldBe null

        val g = Periode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15))
        val ggg = g.intersect(Periode.forMonthOf(LocalDate.of(2024, 1, 1)))
        ggg shouldBe null
    }
})
