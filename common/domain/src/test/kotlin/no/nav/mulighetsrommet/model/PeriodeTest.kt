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
})
