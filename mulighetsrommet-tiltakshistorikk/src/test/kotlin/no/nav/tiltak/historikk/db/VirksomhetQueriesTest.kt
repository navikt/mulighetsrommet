package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.db.queries.VirksomhetQueries

class VirksomhetQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    test("CRUD") {
        val virksomhet = VirksomhetDbo(
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Test Virksomhet",
            organisasjonsform = "AS",
            slettetDato = null,
        )

        database.run {
            var queries = VirksomhetQueries(it)

            queries.upsert(virksomhet)

            val hentet = queries.get(virksomhet.organisasjonsnummer)
            hentet shouldBe virksomhet

            queries.delete(virksomhet.organisasjonsnummer)

            val slettet = queries.get(virksomhet.organisasjonsnummer)
            slettet.shouldBeNull()
        }
    }
})
