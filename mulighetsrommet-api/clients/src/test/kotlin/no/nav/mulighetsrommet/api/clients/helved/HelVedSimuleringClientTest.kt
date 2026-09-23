package no.nav.mulighetsrommet.api.clients.helved

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HelVedSimuleringClientTest : FunSpec({
    val request = HelVedUtbetaling(
        kostnadssted = NavEnhetNummer("1234"),
        id = UUID.randomUUID(),
        sakId = "2026/1234-1",
        behandlingId = "1",
        personIdent = NorskIdent("12345678910"),
        periode = HelVedUtbetaling.Periode(
            fom = LocalDate.of(2026, 9, 23),
            tom = LocalDate.of(2026, 9, 23),
        ),
        belop = 1234,
        tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
        tiltakskode = HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        saksbehandler = no.nav.mulighetsrommet.model.NavIdent("Z123456"),
        beslutter = no.nav.mulighetsrommet.model.NavIdent("Z654321"),
        besluttetTidspunkt = Instant.parse("2026-08-02T10:15:30Z"),
        dryrun = true,
    )

    val responseV2 = HelVedSimuleringResponse.V2(
        perioder = emptyList(),
    )

    val responseInfo = HelVedSimuleringResponse.Info(
        status = HelVedSimuleringResponse.Info.Status.OK_UTEN_ENDRING,
        fagsystem = HelVedSimuleringResponse.Fagsystem.VALP,
        message = "Simulering er vellykket, men det er ingen posteringer å vise til.",
    )

    val responseV1 = HelVedSimuleringResponse.V1(
        oppsummeringer = emptyList(),
        detaljer = HelVedSimuleringResponse.V1.SimuleringDetaljer(
            gjelderId = "12345678910",
            datoBeregnet = LocalDate.of(2026, 4, 2),
            totalBeløp = 1234,
            perioder = emptyList(),
        ),
    )

    test("simulerer med M2M token og serialiserer VALP request mot api/simulering") {
        val engine = createMockEngine {
            post("/api/simulering") { httpRequest ->
                httpRequest.headers[HttpHeaders.Authorization] shouldBe "Bearer m2m-token"
                requestBody<HelVedUtbetaling>(httpRequest) shouldBe request
                respondJson<HelVedSimuleringResponse>(responseV2)
            }
        }
        val client = HelVedSimuleringClient(
            baseUrl = "https://helved.test",
            tokenProvider = TokenProvider { accessType ->
                accessType shouldBe AccessType.M2M
                "m2m-token"
            },
            clientEngine = engine,
        )

        client.simuler(request, AccessType.M2M).shouldBeRight(responseV2)
    }

    test("simulerer info-respons mot api/simulering") {
        val engine = createMockEngine {
            post("/api/simulering") { httpRequest ->
                httpRequest.headers[HttpHeaders.Authorization] shouldBe "Bearer obo-token"
                requestBody<HelVedUtbetaling>(httpRequest) shouldBe request
                respondJson<HelVedSimuleringResponse>(responseInfo)
            }
        }
        val client = HelVedSimuleringClient(
            baseUrl = "https://helved.test",
            tokenProvider = TokenProvider { accessType ->
                when (accessType) {
                    AccessType.M2M -> "m2m-token"
                    is AccessType.OBO -> "obo-token"
                }
            },
            clientEngine = engine,
        )

        client.simuler(request, AccessType.OBO.AzureAd("jwt")).shouldBeRight(responseInfo)
    }

    test("simulerer v1-respons mot api/simulering") {
        val engine = createMockEngine {
            post("/api/simulering") {
                respondJson<HelVedSimuleringResponse>(responseV1)
            }
        }
        val client = HelVedSimuleringClient(
            baseUrl = "https://helved.test",
            tokenProvider = TokenProvider { "token" },
            clientEngine = engine,
        )

        client.simuler(request, AccessType.M2M).shouldBeRight(responseV1)
    }

    test("mappar bad request til feil") {
        val engine = createMockEngine {
            post("/api/simulering") {
                respondJson("bad request", status = HttpStatusCode.BadRequest)
            }
        }
        val client = HelVedSimuleringClient(
            baseUrl = "https://helved.test",
            tokenProvider = TokenProvider { "token" },
            clientEngine = engine,
        )

        client.simuler(request, AccessType.M2M).shouldBeLeft(HelVedSimuleringsError.BadRequest)
    }
})

private inline fun <reified T> requestBody(request: HttpRequestData): T {
    return JsonIgnoreUnknownKeys.decodeFromString((request.body as io.ktor.http.content.TextContent).text)
}
