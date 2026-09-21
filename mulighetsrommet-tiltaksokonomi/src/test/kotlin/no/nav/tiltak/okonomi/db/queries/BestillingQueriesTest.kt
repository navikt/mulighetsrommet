package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.test.Fixtures
import java.time.Instant

class BestillingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val bestilling = Fixtures.bestilling
    val bestillingsnummer = bestilling.bestillingsnummer

    test("opprett bestilling") {
        database.runAndRollback {
            val queries = BestillingQueries(it)

            queries.insertBestilling(bestilling)

            queries.getByBestillingsnummer(bestillingsnummer) shouldBe bestilling
        }
    }

    test("set status") {
        database.runAndRollback { session ->
            val queries = BestillingQueries(session)

            queries.insertBestilling(bestilling)

            queries.getByBestillingsnummer(bestillingsnummer).shouldNotBeNull().should {
                it.status shouldBe BestillingStatusType.AKTIV
            }

            queries.setStatus(bestillingsnummer, BestillingStatusType.ANNULLERT)

            queries.getByBestillingsnummer(bestillingsnummer).shouldNotBeNull().should {
                it.status shouldBe BestillingStatusType.ANNULLERT
            }
        }
    }

    test("annuller bestilling") {
        database.runAndRollback { session ->
            val queries = BestillingQueries(session)

            queries.insertBestilling(bestilling)

            val annullering = Bestilling.Totrinnskontroll(
                behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.TILTAKSADMINISTRASJON),
                behandletTidspunkt = Instant.parse("2025-01-03T00:00:00Z"),
                besluttetAv = OkonomiPart.NavAnsatt(NavIdent("Z123456")),
                besluttetTidspunkt = Instant.parse("2025-01-04T00:00:00Z"),
            )
            queries.setAnnullering(bestillingsnummer, annullering)

            queries.getByBestillingsnummer(bestillingsnummer).shouldNotBeNull().should {
                it.annullering shouldBe annullering
            }
        }
    }
})
