package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

class OebsServiceTest : FunSpec({

    context("splitBelopByMonthsInPeriode") {
        test("splitter beløpet fordelt på antall dager per måned i perioden") {
            val bestillingsperiode = Periode(
                start = LocalDate.of(2023, 2, 1),
                slutt = LocalDate.of(2023, 4, 15),
            )

            val result = splitBelopByMonthsInPeriode(bestillingsperiode, 3000)

            result shouldBe listOf(
                OebsBestillingMelding.Linje(
                    linjeNummer = 1,
                    periode = 2,
                    antall = 1152,
                    startDato = LocalDate.of(2023, 2, 1),
                    sluttDato = LocalDate.of(2023, 2, 28),
                ),
                OebsBestillingMelding.Linje(
                    linjeNummer = 2,
                    periode = 3,
                    antall = 1273,
                    startDato = LocalDate.of(2023, 3, 1),
                    sluttDato = LocalDate.of(2023, 3, 31),
                ),
                OebsBestillingMelding.Linje(
                    linjeNummer = 3,
                    periode = 4,
                    antall = 575,
                    startDato = LocalDate.of(2023, 4, 1),
                    sluttDato = LocalDate.of(2023, 4, 14),
                ),
            )
        }

        test("restbeløpet legges på den første måneden") {
            val bestillingsperiode = Periode(
                start = LocalDate.of(2023, 1, 1),
                slutt = LocalDate.of(2023, 3, 1),
            )

            val result = splitBelopByMonthsInPeriode(bestillingsperiode, 3)

            result shouldBe listOf(
                OebsBestillingMelding.Linje(
                    linjeNummer = 1,
                    periode = 1,
                    antall = 2,
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2023, 1, 31),
                ),
                OebsBestillingMelding.Linje(
                    linjeNummer = 2,
                    periode = 2,
                    antall = 1,
                    startDato = LocalDate.of(2023, 2, 1),
                    sluttDato = LocalDate.of(2023, 2, 28),
                ),
            )
        }
    }
})
