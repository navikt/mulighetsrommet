package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.poao_tilgang.PoaoTilgangClient
import no.nav.mulighetsrommet.ktor.exception.StatusException

class PoaoTilgangServiceTest : FunSpec({
    context("verifyAccessToModia") {
        test("should throw StatusException when decision is DENY") {
            val engine = mockJsonResponse {
                PoaoTilgangClient.TilgangTilModiaResponse(
                    decision = PoaoTilgangClient.Decision(
                        type = PoaoTilgangClient.Decision.DecisionType.DENY,
                        message = null,
                        reason = null,
                    )
                )
            }
            val client = PoaoTilgangClient(engine = engine, baseUrl = "http://poao-tilgang") { "Bearer token" }

            val service = PoaoTilgangService(client)

            shouldThrow<StatusException> {
                service.verifyAccessToModia("ABC123")
            }

            engine.requestHistory shouldHaveSize 1
        }

        test("should verify access to modia when decision is PERMIT") {
            val engine = mockJsonResponse {
                PoaoTilgangClient.TilgangTilModiaResponse(
                    decision = PoaoTilgangClient.Decision(
                        type = PoaoTilgangClient.Decision.DecisionType.PERMIT,
                        message = null,
                        reason = null,
                    )
                )
            }
            val client = PoaoTilgangClient(engine = engine, baseUrl = "http://poao-tilgang") { "Bearer token" }

            val service = PoaoTilgangService(client)

            service.verifyAccessToModia("ABC123")

            engine.requestHistory shouldHaveSize 1
        }

        test("should cache response based on provided NAVident") {
            val engine = mockJsonResponse {
                PoaoTilgangClient.TilgangTilModiaResponse(
                    decision = PoaoTilgangClient.Decision(
                        type = PoaoTilgangClient.Decision.DecisionType.PERMIT,
                        message = null,
                        reason = null,
                    )
                )
            }
            val client = PoaoTilgangClient(engine = engine, baseUrl = "http://poao-tilgang") { "Bearer token" }

            val service = PoaoTilgangService(client)

            service.verifyAccessToModia("ABC111")
            service.verifyAccessToModia("ABC111")

            engine.requestHistory shouldHaveSize 1

            service.verifyAccessToModia("ABC222")
            service.verifyAccessToModia("ABC222")

            engine.requestHistory shouldHaveSize 2
        }
    }
})

private inline fun <reified T> mockJsonResponse(crossinline createResponse: () -> T) = MockEngine {
    val response = createResponse()
    respond(
        content = Json.encodeToString(response),
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}
