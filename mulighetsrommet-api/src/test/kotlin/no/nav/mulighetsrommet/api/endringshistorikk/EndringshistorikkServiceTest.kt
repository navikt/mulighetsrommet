package no.nav.mulighetsrommet.api.endringshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
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
                user = EndretAv.Arena,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "ENDRET",
                user = EndretAv.System,
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "SLETTET",
                user = EndretAv.System,
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
        val ansatt1 = NavAnsattFixture.ansatt1
        val ansatt2 = NavAnsattFixture.ansatt2
        val domain = MulighetsrommetTestDomain(ansatte = listOf(ansatt1, ansatt2))

        database.runAndRollback { session ->
            domain.setup(session)

            val queries = EndringshistorikkQueries(session)

            val id = UUID.randomUUID()

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "OPPRETTET",
                user = EndretAv.NavAnsatt(ansatt1.navIdent),
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

            queries.logEndring(
                DocumentClass.AVTALE,
                operation = "ENDRET",
                user = EndretAv.NavAnsatt(ansatt2.navIdent),
                documentId = id,
                timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
            ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

            queries.getEndringshistorikk(DocumentClass.AVTALE, id) shouldBe EndringshistorikkDto(
                entries = listOf(
                    EndringshistorikkDto.Entry(
                        id = id,
                        operation = "ENDRET",
                        editedAt = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
                        editedBy = EndringshistorikkDto.NavAnsatt(navIdent = "DD2", navn = "Dolly Duck"),
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
})
