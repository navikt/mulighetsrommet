package no.nav.mulighetsrommet.api.clients.kontoregister

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.model.Organisasjonsnummer

class KontoregisterOrganisasjonClientTest : FunSpec({

    val mockEngine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/123456789" -> {
                respond(
                    content = """{"mottaker": "Test Org", "kontonr": "1234.56.78901"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/000000000" -> {
                respond(
                    content = """{"feilmelding": "Not Found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

            else -> {
                respond(
                    content = """{"feilmelding": "Error"}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }
    }

    val client = KontoregisterOrganisasjonClient(
        baseUrl = "http://localhost",
        tokenProvider = { "token" },
        clientEngine = mockEngine,
    )

    test("Should return KontonummerResponse for valid organisasjonsnummer") {
        runBlocking {
            val result = client.getKontonummerForOrganisasjon(
                Organisasjonsnummer("123456789"),
            )
            result shouldBeRight KontonummerResponse("Test Org", "1234.56.78901")
        }
    }

    test("Should return FantIkkeKontonummer error for non-existent organisasjonsnummer") {
        runBlocking {
            val result = client.getKontonummerForOrganisasjon(
                Organisasjonsnummer("000000000"),
            )
            result shouldBeLeft KontonummerRegisterOrganisasjonError.FantIkkeKontonummer
        }
    }

    test("Should return Error for server error") {
        runBlocking {
            val result = client.getKontonummerForOrganisasjon(
                Organisasjonsnummer("999999999"),
            )
            result shouldBeLeft KontonummerRegisterOrganisasjonError.Error
        }
    }
})
