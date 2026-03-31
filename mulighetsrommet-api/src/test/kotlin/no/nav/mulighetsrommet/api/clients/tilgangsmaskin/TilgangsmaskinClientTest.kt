package no.nav.mulighetsrommet.api.clients.tilgangsmaskin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class TilgangsmaskinClientTest : FunSpec({
    val norskIdent = NorskIdent("12345678901")
    val obo = AccessType.OBO.AzureAd("obo-token")

    fun createClient(engine: MockEngine) = TilgangsmaskinClient(
        baseUrl = "https://localhost",
        tokenProvider = { "token" },
        clientEngine = engine,
    )

    test("bulk - 207 Multi-Status returnerer resultater") {
        val responseJson = """
            {
              "ansattId": "Z990883",
              "resultater": [
                {
                  "brukerId": "08526835671",
                  "status": 204
                },
                {
                  "brukerId": "03508331575",
                  "status": 403,
                  "detaljer": {
                    "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                    "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                    "status": 403,
                    "instance": "Z990883/03508331575",
                    "brukerIdent": "03508331575",
                    "navIdent": "Z990883",
                    "begrunnelse": "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                    "traceId": "f85c9caa87a57b6dfde1068ce97f10a5",
                    "kanOverstyres": false
                  }
                },
                {
                  "brukerId": "01011111111",
                  "status": 204
                }
              ]
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = responseJson,
                status = HttpStatusCode.MultiStatus,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = createClient(engine)
        val identer = listOf(
            NorskIdent("08526835671"),
            NorskIdent("03508331575"),
            NorskIdent("01011111111"),
        )

        val result = client.bulk(identer, obo)

        result.resultater shouldHaveSize 3
        result.resultater[0].brukerId shouldBe "08526835671"
        result.resultater[0].status shouldBe 204
        result.resultater[1].brukerId shouldBe "03508331575"
        result.resultater[1].status shouldBe 403
        result.resultater[2].brukerId shouldBe "01011111111"
        result.resultater[2].status shouldBe 204
    }

    test("bulk - 404 kaster exception") {
        val responseJson = """
            {
              "detail": "Fant ingen oid for navident A222222, er den fremdeles gyldig?",
              "instance": "/api/v1/bulk/A222222/KJERNE_REGELTYPE",
              "status": 404,
              "title": "Uventet respons fra Entra",
              "navident": "A222222"
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = responseJson,
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = createClient(engine)

        shouldThrow<Exception> {
            client.bulk(listOf(norskIdent), obo)
        }.message shouldContain "Nav Ident ikke funnet"
    }

    test("bulk - 413 kaster exception") {
        val responseJson = """
            {
              "ansattId": "string",
              "resultater": [
                {
                  "brukerId": "string",
                  "detaljer": "string",
                  "status": 0
                }
              ]
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = responseJson,
                status = HttpStatusCode.PayloadTooLarge,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = createClient(engine)

        shouldThrow<Exception> {
            client.bulk(listOf(norskIdent), obo)
        }.message shouldContain "For mange brukere"
    }

    test("bulk - uventet statuskode kaster exception") {
        val engine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
            )
        }

        val client = createClient(engine)

        shouldThrow<Exception> {
            client.bulk(listOf(norskIdent), obo)
        }.message shouldContain "Feil mot tilgangsmaskinen"
    }
})
