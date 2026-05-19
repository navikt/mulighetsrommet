package no.nav.mulighetsrommet.altinn.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet1
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet2
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Instant

class AltinnRettigheterQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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
            queries.altinnRettigheter.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet1),
                expiry = Instant.parse("2024-01-01T00:00:00Z"),
            )
            queries.altinnRettigheter.upsertRettigheter(
                norskIdent = norskIdent2,
                bedriftRettigheter = listOf(rettighetUnderenhet2),
                expiry = Instant.parse("2024-01-01T00:00:00Z"),
            )

            queries.altinnRettigheter.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet1.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = Instant.parse("2024-01-01T00:00:00Z"),
                        ),
                    ),
                ),
            )

            queries.altinnRettigheter.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet1, rettighetUnderenhet2),
                expiry = Instant.parse("2025-01-01T00:00:00Z"),
            )

            queries.altinnRettigheter.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet1.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = Instant.parse("2025-01-01T00:00:00Z"),
                        ),
                    ),
                ),
                BedriftRettigheterDbo(
                    underenhet2.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = Instant.parse("2025-01-01T00:00:00Z"),
                        ),
                    ),
                ),
            )
        }
    }

    test("nye rettigheter sletter gamle") {
        database.runAndRollback {
            queries.altinnRettigheter.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet1),
                expiry = Instant.parse("2024-01-01T00:00:00Z"),
            )
            queries.altinnRettigheter.upsertRettigheter(
                norskIdent = norskIdent1,
                bedriftRettigheter = listOf(rettighetUnderenhet2),
                expiry = Instant.parse("2024-01-01T00:00:00Z"),
            )

            queries.altinnRettigheter.getRettigheter(norskIdent1) shouldContainExactly listOf(
                BedriftRettigheterDbo(
                    underenhet2.organisasjonsnummer,
                    listOf(
                        BedriftRettighetWithExpiry(
                            rettighet = AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING,
                            expiry = Instant.parse("2024-01-01T00:00:00Z"),
                        ),
                    ),
                ),
            )
        }
    }
})
