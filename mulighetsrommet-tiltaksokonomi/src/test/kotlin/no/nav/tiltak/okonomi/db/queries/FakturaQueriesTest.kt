package no.nav.tiltak.okonomi.db.queries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OebsFeil
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.test.Fixtures

class FakturaQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val bestilling = Fixtures.bestilling

    val faktura = Fixtures.faktura
    val fakturanummer = faktura.fakturanummer

    test("opprett faktura") {
        database.runAndRollback {
            val bestillingQueries = BestillingQueries(it)
            bestillingQueries.insertBestilling(bestilling)

            val fakturaQueries = FakturaQueries(it)

            fakturaQueries.insertFaktura(faktura)

            fakturaQueries.getByFakturanummer(fakturanummer) shouldBe faktura
        }
    }

    test("bevarer OeBS-feil ved lagring og uthenting") {
        database.runAndRollback { session ->
            BestillingQueries(session).insertBestilling(bestilling)
            val queries = FakturaQueries(session)
            val feilet = faktura.copy(
                status = FakturaStatusType.FEILET,
                feil = OebsFeil(kode = "FEILKODE", melding = "Fakturaen ble avvist"),
            )

            queries.insertFaktura(feilet)

            queries.getByFakturanummer(fakturanummer).shouldNotBeNull().feil shouldBe
                OebsFeil(kode = "FEILKODE", melding = "Fakturaen ble avvist")
        }
    }
})
