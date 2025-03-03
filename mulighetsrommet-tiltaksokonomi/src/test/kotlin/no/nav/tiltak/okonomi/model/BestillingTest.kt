package no.nav.tiltak.okonomi.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate

class BestillingTest : FunSpec({

    context("fromOpprettBestilling") {
        val opprettBestilling = OpprettBestilling(
            bestillingsnummer = "2025/1",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            arrangor = OpprettBestilling.Arrangor(
                hovedenhet = Organisasjonsnummer("123456789"),
                underenhet = Organisasjonsnummer("234567891"),
            ),
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
        )

        test("felter utledes fra OpprettBestilling") {
            val bestilling = Bestilling.fromOpprettBestilling(opprettBestilling)

            bestilling.status shouldBe BestillingStatusType.BESTILT
            bestilling.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
            bestilling.arrangorHovedenhet shouldBe Organisasjonsnummer("123456789")
            bestilling.arrangorUnderenhet shouldBe Organisasjonsnummer("234567891")
            bestilling.opprettelse.behandletAv shouldBe OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
            bestilling.opprettelse.behandletTidspunkt shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
            bestilling.opprettelse.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavIdent("Z123456"))
            bestilling.opprettelse.besluttetTidspunkt shouldBe LocalDate.of(2025, 1, 2).atStartOfDay()
            bestilling.annullering.shouldBeNull()
        }

        test("utleder bestillingslinjer for hver måned i bestillingens periode") {
            val bestilling1 = Bestilling.fromOpprettBestilling(
                opprettBestilling.copy(periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))),
            )
            bestilling1.linjer shouldBe listOf(
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

            val bestilling2 = Bestilling.fromOpprettBestilling(
                opprettBestilling.copy(periode = Periode(LocalDate.of(2025, 7, 15), LocalDate.of(2025, 8, 15))),
            )
            bestilling2.linjer shouldBe listOf(
                Bestilling.Linje(
                    linjenummer = 1,
                    periode = Periode(LocalDate.of(2025, 7, 15), LocalDate.of(2025, 8, 1)),
                    belop = 549,
                ),
                Bestilling.Linje(
                    linjenummer = 2,
                    periode = Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15)),
                    belop = 451,
                ),
            )
        }
    }
})
