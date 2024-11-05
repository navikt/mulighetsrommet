package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class ArrangorflateRoutesTest : FunSpec({
    val databaseConfig = createDatabaseTestConfig()
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain(
        enheter = emptyList(),
        ansatte = emptyList(),
        tiltakstyper = emptyList(),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
        deltakere = emptyList(),
        arrangorer = listOf(
            ArrangorDto(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("973674471"),
                navn = "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
                postnummer = "0102",
                poststed = "Oslo",
            ),
        ),
    )
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = emptyList()),
        engine = engine,
    )

    test("401 Unauthorized uten pid i claims") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 og arrangor returneres på tilgang endepunkt") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.OK
            val responseBody = response.bodyAsText()
            val tilganger: List<ArrangorDto> = Json.decodeFromString(responseBody)
            tilganger shouldHaveSize 1
            tilganger[0].organisasjonsnummer shouldBe Organisasjonsnummer("973674471")
        }
    }
})
