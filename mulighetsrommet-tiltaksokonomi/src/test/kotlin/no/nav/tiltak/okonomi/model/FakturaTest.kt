package no.nav.tiltak.okonomi.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.FakturaStatusType
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
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay(),
            gjorOppBestilling = false,
            beskrivelse = "Beskrivelse",
        )

        test("felter utledes fra OpprettFaktura") {
            val bestillingslinjer = listOf(
                Bestilling.Linje(
                    linjenummer = 1,
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                    belop = 526,
                ),
            )

            val faktura = Faktura.fromOpprettFaktura(
                opprettFaktura,
                bestillingslinjer,
            )

            faktura.bestillingsnummer shouldBe "2025/1"
            faktura.fakturanummer shouldBe "2025/1/1"
            faktura.kontonummer shouldBe Kontonummer("12345678901")
            faktura.kid shouldBe null
            faktura.belop shouldBe 1000
            faktura.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            faktura.status shouldBe FakturaStatusType.SENDT
            faktura.behandletAv shouldBe OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
            faktura.behandletTidspunkt shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
            faktura.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavIdent("Z123456"))
            faktura.besluttetTidspunkt shouldBe LocalDate.of(2025, 2, 1).atStartOfDay()
        }

        test("utleder fakturalinje basert på fakturaens periode og bestillingens linjer") {
            val bestillingslinjer = listOf(
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
                bestillingslinjer,
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
                bestillingslinjer,
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
                bestillingslinjer,
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
