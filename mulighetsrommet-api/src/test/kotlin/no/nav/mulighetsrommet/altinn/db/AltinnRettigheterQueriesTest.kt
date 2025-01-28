package no.nav.mulighetsrommet.altinn.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet1
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet2
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDateTime

class AltinnRettigheterQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val norskIdent1 = NorskIdent("12345678901")
    val rettighet1 = PersonBedriftRettigheterDbo(
        norskIdent = norskIdent1,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = underenhet1.organisasjonsnummer,
                rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON),
            ),
        ),
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    val norskIdent2 = NorskIdent("42345678903")
    val rettighet2 = PersonBedriftRettigheterDbo(
        norskIdent = norskIdent2,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = underenhet2.organisasjonsnummer,
                rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON),
            ),
        ),
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    test("CRUD") {
        database.runAndRollback {
            val queries = AltinnRettigheterQueries(it)

            queries.upsertRettighet(rettighet1)
            queries.upsertRettighet(rettighet2)

            queries.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet1.organisasjonsnummer,
                    listOf(
                        RettighetDbo(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_REFUSJON,
                            expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
                        ),
                    ),
                ),
            )
        }
    }
})
