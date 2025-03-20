package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.test.Fixtures

class FakturaQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val bestilling = Fixtures.bestilling

    val faktura = Fixtures.faktura

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
