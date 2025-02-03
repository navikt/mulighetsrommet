package no.nav.mulighetsrommet.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tiltak.okonomi.api.OkonomiPart
import no.nav.mulighetsrommet.tiltak.okonomi.databaseConfig
import no.nav.mulighetsrommet.tiltak.okonomi.db.Bestilling
import no.nav.mulighetsrommet.tiltak.okonomi.db.BestillingStatusType
import no.nav.mulighetsrommet.tiltak.okonomi.db.LinjeDbo
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.Kilde
import java.time.LocalDate

class OebsBestillingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val dbo = Bestilling(
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
        opprettetAv = OkonomiPart.System(Kilde.TILTADM),
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

    test("opprett bestilling") {
        database.runAndRollback {
            val queries = BestillingQueries(it)

            queries.createBestilling(dbo)

            queries.getBestilling("A-1") shouldBe dbo
        }
    }

    test("annuller bestilling") {
        database.runAndRollback {
            val queries = BestillingQueries(it)

            queries.createBestilling(dbo)

            queries.setStatus("A-1", BestillingStatusType.ANNULLERT)

            queries.getBestilling("A-1") shouldBe dbo.copy(status = BestillingStatusType.ANNULLERT)
        }
    }
})
