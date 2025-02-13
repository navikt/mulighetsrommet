package no.nav.tiltak.okonomi.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class DivideBelopByMonthsInPeriodeKtTest : FunSpec({
    test("splitter beløpet fordelt på antall dager per måned i perioden") {
        val bestillingsperiode = Periode(
            start = LocalDate.of(2023, 2, 1),
            slutt = LocalDate.of(2023, 4, 15),
        )

        val perioder = divideBelopByMonthsInPeriode(bestillingsperiode, 3000)

        perioder shouldBe listOf(
            Periode(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 3, 1)) to 1152,
            Periode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1)) to 1273,
            Periode(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 15)) to 575,
        )
    }

    test("restbeløpet legges på den første måneden") {
        val bestillingsperiode = Periode(
            start = LocalDate.of(2023, 1, 1),
            slutt = LocalDate.of(2023, 3, 1),
        )

        val perioder = divideBelopByMonthsInPeriode(bestillingsperiode, 3)

        perioder shouldBe listOf(
            Periode(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)) to 2,
            Periode(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 3, 1)) to 1,
        )
    }
})
