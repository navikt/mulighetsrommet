package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.db.queries.VirksomhetQueries

class VirksomhetQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD for virksomhet") {
        test("upsert og delete virksomhet") {
            val virksomhet = VirksomhetDbo(
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321"),
                navn = "Test Virksomhet",
                organisasjonsform = "AS",
                slettetDato = null,
            )

            database.run {
                VirksomhetQueries(it).upsert(virksomhet)
            }

            database.assertRequest("select * from virksomhet")
                .hasNumberOfRows(1)
                .row()
                .value("organisasjonsnummer").isEqualTo("123456789")
                .value("overordnet_enhet_organisasjonsnummer").isEqualTo("987654321")
                .value("navn").isEqualTo("Test Virksomhet")
                .value("organisasjonsform").isEqualTo("AS")
                .value("slettet_dato").isNull

            database.run {
                VirksomhetQueries(it).delete(virksomhet.organisasjonsnummer)
            }

            database.assertRequest("select * from virksomhet").hasNumberOfRows(0)
        }
    }
})
