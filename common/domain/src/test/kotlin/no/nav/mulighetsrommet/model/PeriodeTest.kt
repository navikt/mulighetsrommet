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

        period.getLastInclusiveDate() shouldBe LocalDate.of(2021, 1, 30)
    }

    test("should check if date is in period") {
        val period = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        (LocalDate.of(2021, 1, 1) in period) shouldBe true
        (LocalDate.of(2021, 1, 15) in period) shouldBe true
        (LocalDate.of(2021, 1, 31) in period) shouldBe false
    }

    test("should check if period is in period") {
        val period = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

        (Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31)) in period) shouldBe true
        (Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 16)) in period) shouldBe true
        (Periode(LocalDate.of(2021, 1, 31), LocalDate.of(2021, 2, 1)) in period) shouldBe false
        (Periode(LocalDate.of(2020, 12, 31), LocalDate.of(2021, 1, 1)) in period) shouldBe false
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

    test("should check if periods overlap") {
        val period1 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val period2 = Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 2, 15))
        period1.overlaps(period2) shouldBe true

        val period3 = Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 15))
        period1.overlaps(period3) shouldBe false
    }

    test("should intersect periods") {
        val period1 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val period2 = Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 2, 15))
        period1.intersect(period2) shouldBe Periode(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 31))

        val period3 = Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 15))
        period1.intersect(period3) shouldBe null
    }

    test("should subtract periods correctly in various cases") {
        fun period(start: String, end: String) = Periode(LocalDate.parse(start), LocalDate.parse(end))

        // Case 1: No overlap
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(period("2024-02-01", "2024-02-28")),
        ) shouldBe listOf(period("2024-03-01", "2024-03-31"))

        // Case 4: Exclusion overlaps at start
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(period("2024-03-01", "2024-03-05")),
        ) shouldBe listOf(period("2024-03-05", "2024-03-31"))

        // Case 5: Exclusion overlaps at end
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(period("2024-03-25", "2024-03-31")),
        ) shouldBe listOf(period("2024-03-01", "2024-03-25"))

        // Case 6: Exclusion fully inside
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(period("2024-03-10", "2024-03-20")),
        ) shouldBe listOf(
            period("2024-03-01", "2024-03-10"),
            period("2024-03-20", "2024-03-31"),
        )

        // Case 7: Multiple exclusions
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(
                period("2024-03-05", "2024-03-10"),
                period("2024-03-15", "2024-03-20"),
            ),
        ) shouldBe listOf(
            period("2024-03-01", "2024-03-05"),
            period("2024-03-10", "2024-03-15"),
            period("2024-03-20", "2024-03-31"),
        )

        // Case 8: Exclusion exactly matches full period
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(period("2024-03-01", "2024-03-31")),
        ) shouldBe emptyList()

        // Case 9: Adjacent exclusions
        period("2024-03-01", "2024-03-31").subtractPeriods(
            listOf(
                period("2024-03-05", "2024-03-10"),
                period("2024-03-10", "2024-03-15"),
            ),
        ) shouldBe listOf(
            period("2024-03-01", "2024-03-05"),
            period("2024-03-15", "2024-03-31"),
        )
    }
})
