package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDateTime
import java.util.*

class EndringshistorikkServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    test("opprett og les endringshistorikk sortert med nyeste endringer først") {
        val id = UUID.randomUUID()

        val endringshistorikk = EndringshistorikkService(database.db)

        endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation = "OPPRETTET",
            userId = "Arena",
            documentId = id,
            timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
        ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

        endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation = "ENDRET",
            userId = "Ola",
            documentId = id,
            timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
        ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

        endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation = "SLETTET",
            userId = "Arena",
            documentId = id,
            timestamp = LocalDateTime.of(2023, 1, 3, 9, 0, 0),
        ) { JsonNull }

        endringshistorikk.getEndringshistorikk(DocumentClass.AVTALE, id) shouldBe EndringshistorikkDto(
            entries = listOf(
                EndringshistorikkDto.Entry(
                    id = id,
                    operation = "SLETTET",
                    editedAt = LocalDateTime.of(2023, 1, 3, 9, 0, 0),
                    editedBy = EndringshistorikkDto.Systembruker(navn = "Arena"),
                ),
                EndringshistorikkDto.Entry(
                    id = id,
                    operation = "ENDRET",
                    editedAt = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
                    editedBy = EndringshistorikkDto.Systembruker(navn = "Ola"),
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

    test("bruker i endringshistorikk blir utledet fra kjente Nav-ansatte") {
        val id = UUID.randomUUID()

        val ansatt1 = NavAnsattFixture.ansatt1
        val ansatt2 = NavAnsattFixture.ansatt2

        val domain = MulighetsrommetTestDomain(ansatte = listOf(ansatt1, ansatt2))
        domain.initialize(database.db)

        val endringshistorikk = EndringshistorikkService(database.db)

        endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation = "OPPRETTET",
            userId = ansatt1.navIdent.value,
            documentId = id,
            timestamp = LocalDateTime.of(2023, 1, 1, 9, 0, 0),
        ) { Json.parseToJsonElement("""{ "navn": "Ny avtale" }""") }

        endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation = "ENDRET",
            userId = ansatt2.navIdent.value,
            documentId = id,
            timestamp = LocalDateTime.of(2023, 1, 2, 9, 0, 0),
        ) { Json.parseToJsonElement("""{ "navn": "Endret avtale" }""") }

        endringshistorikk.getEndringshistorikk(DocumentClass.AVTALE, id) shouldBe EndringshistorikkDto(
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
})
