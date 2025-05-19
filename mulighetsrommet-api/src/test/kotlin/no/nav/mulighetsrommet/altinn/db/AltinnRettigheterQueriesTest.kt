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
import java.time.ZoneOffset

class AltinnRettigheterQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val norskIdent1 = NorskIdent("12345678901")
    val norskIdent2 = NorskIdent("42345678903")

    val rettighetUnderenhet1 = BedriftRettigheter(
        organisasjonsnummer = underenhet1.organisasjonsnummer,
        rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING),
    )
    val rettighetUnderenhet2 = BedriftRettigheter(
        organisasjonsnummer = underenhet2.organisasjonsnummer,
        rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING),
    )

    test("upsert and get rettigheter for person") {
        database.runAndRollback {
            val queries = AltinnRettigheterQueries(it)

            queries.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet1),
                expiry = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            )
            queries.upsertRettigheter(
                norskIdent = norskIdent2,
                bedriftRettigheter = listOf(rettighetUnderenhet2),
                expiry = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            )

            queries.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet1.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                        ),
                    ),
                ),
            )

            queries.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet1, rettighetUnderenhet2),
                expiry = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            )

            queries.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet1.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                        ),
                    ),
                ),
                BedriftRettigheterDbo(
                    underenhet2.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                        ),
                    ),
                ),
            )
        }
    }
})
