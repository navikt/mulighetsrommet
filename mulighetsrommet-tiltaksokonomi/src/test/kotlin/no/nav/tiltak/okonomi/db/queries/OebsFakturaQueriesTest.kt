package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.db.*
import java.time.LocalDate

class OebsFakturaQueriesTest : FunSpec({
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
        opprettetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
        opprettetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
        besluttetTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        linjer = listOf(
            LinjeDbo(
                linjenummer = 1,
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 2, 1),
                ),
                belop = 500,
            ),
            LinjeDbo(
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
        val faktura = Faktura(
            fakturanummer = "4567",
            bestillingsnummer = "A-1",
            kontonummer = Kontonummer("12345678901"),
            kid = Kid("123123123123123"),
            belop = 500,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            status = FakturaStatusType.UTBETALT,
            opprettetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            opprettetTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
            besluttetTidspunkt = LocalDate.of(2025, 2, 2).atStartOfDay(),
            linjer = listOf(
                LinjeDbo(
                    linjenummer = 1,
                    periode = Periode(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 2, 1),
                    ),
                    belop = 500,
                ),
                LinjeDbo(
                    linjenummer = 2,
                    periode = Periode(
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2025, 3, 1),
                    ),
                    belop = 500,
                ),
            ),
        )

        database.runAndRollback {
            val bestillingQueries = BestillingQueries(it)
            bestillingQueries.createBestilling(bestilling)

            val fakturaQueries = FakturaQueries(it)

            fakturaQueries.opprettFaktura(faktura)

            fakturaQueries.getFaktura("4567") shouldBe faktura
        }
    }
})
