package no.nav.tiltak.okonomi.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.OpprettBestilling
import java.time.LocalDate

class BestillingTest : FunSpec({

    context("fromOpprettBestilling") {
        val opprettBestilling = OpprettBestilling(
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            arrangor = OpprettBestilling.Arrangor(
                hovedenhet = Organisasjonsnummer("123456789"),
                underenhet = Organisasjonsnummer("234567891"),
            ),
            bestillingsnummer = "2025/1",
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
        )

        test("utleder bestillingslinjer for hver måned i bestillingens periode") {
            val bestilling1 = Bestilling.fromOpprettBestilling(
                opprettBestilling.copy(periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))),
                BestillingStatusType.AKTIV,
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
                BestillingStatusType.AKTIV,
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
