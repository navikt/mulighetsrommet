package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.BestillingStatusType
import java.time.LocalDate

class BestillingQueriesTest : FunSpec({
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

    test("opprett bestilling") {
        database.runAndRollback {
            val queries = BestillingQueries(it)

            queries.insertBestilling(bestilling)

            queries.getByBestillingsnummer("A-1") shouldBe bestilling
        }
    }

    test("annuller bestilling") {
        database.runAndRollback { session ->
            val queries = BestillingQueries(session)

            queries.insertBestilling(bestilling)

            val annullering = Bestilling.Totrinnskontroll(
                behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                behandletTidspunkt = LocalDate.of(2025, 1, 3).atStartOfDay(),
                besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                besluttetTidspunkt = LocalDate.of(2025, 1, 4).atStartOfDay(),
            )
            queries.setAnnullert("A-1", annullering)

            queries.getByBestillingsnummer("A-1").shouldNotBeNull().should {
                it.status shouldBe BestillingStatusType.ANNULLERT
                it.annullering shouldBe annullering
            }
        }
    }
})
