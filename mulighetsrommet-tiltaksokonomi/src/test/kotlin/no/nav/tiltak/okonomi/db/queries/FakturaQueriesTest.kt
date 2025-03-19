package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.BestillingStatusType
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.FakturaStatusType
import java.time.LocalDate

class FakturaQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val bestilling = Bestilling(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arrangorHovedenhet = Organisasjonsnummer("123456789"),
        arrangorUnderenhet = Organisasjonsnummer("234567890"),
        kostnadssted = NavEnhetNummer("0400"),
        bestillingsnummer = "A-1",
        avtalenummer = null,
        belop = 1000,
        periode = Periode(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 3, 1),
        ),
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
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 1),
                ),
                belop = 500,
            ),
            Bestilling.Linje(
                linjenummer = 2,
                periode = Periode(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                ),
                belop = 500,
            ),
        ),
    )

    val faktura = Faktura(
        fakturanummer = "4567",
        bestillingsnummer = "A-1",
        kontonummer = Kontonummer("12345678901"),
        kid = Kid("123123123123123"),
        belop = 500,
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        status = FakturaStatusType.SENDT,
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
    )

    test("opprett faktura") {
        database.runAndRollback {
            val bestillingQueries = BestillingQueries(it)
            bestillingQueries.insertBestilling(bestilling)

            val fakturaQueries = FakturaQueries(it)

            fakturaQueries.insertFaktura(faktura)

            fakturaQueries.getByFakturanummer("4567") shouldBe faktura
        }
    }
})
