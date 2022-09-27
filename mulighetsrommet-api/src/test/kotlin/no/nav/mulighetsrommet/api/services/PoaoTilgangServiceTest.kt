package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.poao_tilgang.api.dto.response.DecisionDto
import no.nav.poao_tilgang.api.dto.response.DecisionType
import no.nav.poao_tilgang.api.dto.response.EvaluatePoliciesResponse
import no.nav.poao_tilgang.api.dto.response.PolicyEvaluationResultDto
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import java.util.*

class PoaoTilgangServiceTest : FunSpec({
    context("verifyAccessToModia") {
        test("should throw StatusException when decision is DENY") {
            val engine = mockJsonResponse {
                EvaluatePoliciesResponse(
                    results = listOf(
                        PolicyEvaluationResultDto(
                            UUID.randomUUID(),
                            decision = DecisionDto(type = DecisionType.DENY, message = null, reason = null)
                        )
                    )
                )
            }

            val client: PoaoTilgangClient =
                PoaoTilgangHttpClient(baseUrl = "http://poao-tilgang", { "" }, client = mockClient)

            val service = PoaoTilgangService(client)

            shouldThrow<StatusException> {
                service.verifyAccessToUserFromVeileder("ABC123", "12345678910")
            }

            engine.requestHistory shouldHaveSize 1
        }

        test("should verify access to modia when decision is PERMIT") {
            val engine = mockJsonResponse {
                PoaoTilgangClient.TilgangTilModiaResponse(
                    decision = PoaoTilgangClient.Decision(
                        type = PoaoTilgangClient.Decision.DecisionType.PERMIT,
                        message = null,
                        reason = null
                    )
                )
            }
            val client = PoaoTilgangClient(engine = engine, baseUrl = "http://poao-tilgang") { "Bearer token" }

            val service = PoaoTilgangService(client)

            service.verifyAccessToUserFromVeileder("ABC123", "12345678910")

            engine.requestHistory shouldHaveSize 1
        }

        test("should cache response based on provided NAVident") {
            val engine = mockJsonResponse {
                PoaoTilgangClient.TilgangTilModiaResponse(
                    decision = PoaoTilgangClient.Decision(
                        type = PoaoTilgangClient.Decision.DecisionType.PERMIT,
                        message = null,
                        reason = null
                    )
                )
            }
            val client = PoaoTilgangClient(engine = engine, baseUrl = "http://poao-tilgang") { "Bearer token" }

            val service = PoaoTilgangService(client)

            service.verifyAccessToUserFromVeileder("ABC111", "12345678910")
            service.verifyAccessToUserFromVeileder("ABC111", "12345678910")

            engine.requestHistory shouldHaveSize 1

            service.verifyAccessToUserFromVeileder("ABC222", "12345678910")
            service.verifyAccessToUserFromVeileder("ABC222", "12345678910")

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
