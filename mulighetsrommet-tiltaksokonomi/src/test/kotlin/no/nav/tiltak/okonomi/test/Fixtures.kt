package no.nav.tiltak.okonomi.test

import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import java.time.LocalDate
import java.time.LocalDateTime

object Fixtures {

    val bestilling = Bestilling(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("123456789"),
        arrangorUnderenhet = Organisasjonsnummer("234567890"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = "A-1",
        avtalenummer = null,
        belop = 1000,
        periode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 1)),
        status = BestillingStatusType.AKTIV,
        opprettelse = Bestilling.Totrinnskontroll(
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        ),
        annullering = null,
        linjer = listOf(
            Bestilling.Linje(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
            Bestilling.Linje(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 4, 1),
                ),
                belop = 500,
            ),
        ),
    )

    val faktura = Faktura(
        fakturanummer = "4567",
        bestillingsnummer = "A-1",
        kontonummer = Kontonummer("12345678901"),
        kid = Kid("123123123123120"),
        belop = 500,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        status = FakturaStatusType.SENDT,
        fakturaStatusSistOppdatert = LocalDateTime.of(2025, 1, 1, 0, 0),
        behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
        behandletTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay(),
        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
        besluttetTidspunkt = LocalDate.of(2025, 2, 2).atStartOfDay(),
        linjer = listOf(
            Faktura.Linje(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 1),
                ),
                belop = 500,
            ),
            Faktura.Linje(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
        ),
        beskrivelse = """
            Tiltakstype: Arbeidsforberedende trening
            Periode: 01.01.2025 - 31.01.2025
            Tilsagnsnummer: A-1
        """.trimIndent(),
    )
}
