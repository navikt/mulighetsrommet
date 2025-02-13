package no.nav.tiltak.okonomi.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.OpprettFaktura
import java.time.LocalDate

class FakturaTest : FunSpec({

    context("fromOpprettBestilling") {
        val opprettFaktura = OpprettFaktura(
            fakturanummer = "2025/1/1",
            bestillingsnummer = "2025/1",
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            ),
            belop = 1000,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            opprettetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            opprettetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        )

        test("utleder fakturalinje basert på fakturaens periode og bestillingens linjer") {
            val bestillingslinjger = listOf(
                Bestilling.Linje(
                    linjenummer = 1,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    belop = 526,
                ),
                Bestilling.Linje(
                    linjenummer = 2,
                    periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 1)),
                    belop = 474,
                ),
            )

            val faktura1 = Faktura.fromOpprettFaktura(
                opprettFaktura.copy(periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))),
                bestillingslinjger,
            )
            faktura1.linjer shouldBe listOf(
                Faktura.Linje(
                    linjenummer = 1,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    belop = 1000,
                ),
            )

            val faktura2 = Faktura.fromOpprettFaktura(
                opprettFaktura.copy(periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 1))),
                bestillingslinjger,
            )
            faktura2.linjer shouldBe listOf(
                Faktura.Linje(
                    linjenummer = 2,
                    periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 1)),
                    belop = 1000,
                ),
            )

            val faktura3 = Faktura.fromOpprettFaktura(
                opprettFaktura.copy(periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15))),
                bestillingslinjger,
            )
            faktura3.linjer shouldBe listOf(
                Faktura.Linje(
                    linjenummer = 1,
                    periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1)),
                    belop = 549,
                ),
                Faktura.Linje(
                    linjenummer = 2,
                    periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 15)),
                    belop = 451,
                ),
            )
        }
    }
})
