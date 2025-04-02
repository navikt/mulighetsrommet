package no.nav.mulighetsrommet.api.endringshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.time.LocalDateTime
import java.util.*

class EndringshistorikkServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    test("opprett og les endringshistorikk sortert med nyeste endringer først") {
        database.runAndRollback {
            val id = UUID.randomUUID()

            val queries = EndringshistorikkQueries(it)

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "ENDRET",
                agent = Tiltaksadministrasjon,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "SLETTET",
                agent = Tiltaksadministrasjon,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 3, 9, 0, 0),
            ) { JsonNull }

            queries.getEndringshistorikk(DocumentClass.AVTALE, id) shouldBe EndringshistorikkDto(
                entries = listOf(
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "SLETTET",
                        editedAt = LocalDateTime.of(2023, 1, 3, 9, 0, 0),
                        editedBy = EndringshistorikkDto.Systembruker(navn = "System"),
                    ),
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "ENDRET",
                        editedAt = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
                        editedBy = EndringshistorikkDto.Systembruker(navn = "System"),
                    ),
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "OPPRETTET",
                        editedAt = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
                        editedBy = EndringshistorikkDto.Systembruker(navn = "Arena"),
                    ),
                ),
            )
        }
    }

    test("bruker i endringshistorikk blir utledet fra kjente Nav-ansatte") {
        val ansatt1 = NavAnsattFixture.DonaldDuck
        val ansatt2 = NavAnsattFixture.MikkeMus
        val domain = MulighetsrommetTestDomain(ansatte = listOf(ansatt1, ansatt2))

        database.runAndRollback { session ->
            domain.setup(session)

            val queries = EndringshistorikkQueries(session)

            val id = UUID.randomUUID()

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "OPPRETTET",
                agent = ansatt1.navIdent,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "ENDRET",
                agent = ansatt2.navIdent,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

            queries.getEndringshistorikk(DocumentClass.AVTALE, id) shouldBe EndringshistorikkDto(
                entries = listOf(
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "ENDRET",
                        editedAt = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
                        editedBy = EndringshistorikkDto.NavAnsatt(navIdent = "DD2", navn = "Mikke Mus"),
                    ),
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "OPPRETTET",
                        editedAt = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
                        editedBy = EndringshistorikkDto.NavAnsatt(navIdent = "DD1", navn = "Donald Duck"),
                    ),
                ),
            )
        }
    }

    test("opprett alle dokument klasser") {
        database.runAndRollback {
            val avtaleId = UUID.randomUUID()
            val tilsagnId = UUID.randomUUID()
            val gjennomforingId = UUID.randomUUID()
            val utbetalingId = UUID.randomUUID()

            val queries = EndringshistorikkQueries(it)

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = avtaleId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.TILSAGN,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = tilsagnId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Nytt tilsagn" }""") }

            queries.logEndring(
                DocumentClass.GJENNOMFORING,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = gjennomforingId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny gjennomforing" }""") }

            queries.logEndring(
                DocumentClass.UTBETALING,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = utbetalingId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny utbetaling" }""") }

            queries.getEndringshistorikk(DocumentClass.AVTALE, avtaleId).entries shouldHaveSize 1
            queries.getEndringshistorikk(DocumentClass.TILSAGN, tilsagnId).entries shouldHaveSize 1
            queries.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforingId).entries shouldHaveSize 1
            queries.getEndringshistorikk(DocumentClass.UTBETALING, utbetalingId).entries shouldHaveSize 1
        }
    }

    test("samtidige endringer") {
        database.runAndRollback {
            val avtaleId = UUID.randomUUID()

            val queries = EndringshistorikkQueries(it)
            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "OPPRETTET",
                agent = Arena,
                documentId = avtaleId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "ENDRET",
                agent = Arena,
                documentId = avtaleId,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

            val entries = queries.getEndringshistorikk(DocumentClass.AVTALE, avtaleId).entries
            entries shouldHaveSize 2
        }
    }
})
