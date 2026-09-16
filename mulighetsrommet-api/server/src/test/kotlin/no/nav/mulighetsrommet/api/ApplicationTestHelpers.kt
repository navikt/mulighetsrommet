package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonaliaResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinRequest
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResponse
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

fun MockOAuth2Server.createRequestWithAnsattClaims(
    ansatt: NavAnsatt,
    roles: Set<EntraGroupNavAnsattRolleMapping>,
): (HttpRequestBuilder) -> Unit = { request: HttpRequestBuilder ->
    val claims = getAnsattClaims(ansatt, roles)
    request.bearerAuth(issueToken(claims = claims).serialize())
}

fun getAnsattClaims(
    ansatt: NavAnsatt,
    roles: Set<EntraGroupNavAnsattRolleMapping>,
): Map<String, Any> {
    return mapOf(
        "NAVident" to ansatt.navIdent.value,
        "oid" to ansatt.entraObjectId,
        "uti" to UUID.randomUUID().toString(),
        "groups" to roles.map { it.entraGroupId.toString() },
    )
}

fun MockEngineBuilder.mockPdlEmptyResult() {
    post("/pdl/graphql") {
        respond(
            content = """{"data":{"hentPersonBolk":[],"hentGeografiskTilknytningBolk":[]},"errors":[]}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
}

fun MockEngineBuilder.mockTilgangsmaskinenForbidden(avvistGrunn: String) {
    post("/tilgangsmaskin/api/v1/bulk/obo") { request ->
        val requests = request.decodeRequestBody<List<TilgangsmaskinRequest>>()
        val responses = TilgangsmaskinResponse(
            requests.map { req ->
                TilgangsmaskinResponse.Resultat(
                    brukerId = req.brukerId,
                    status = 403,
                    detaljer = object : ProblemDetail() {
                        override val type = ""
                        override val title = avvistGrunn
                        override val status = 403
                        override val detail = ""
                        override val instance = null
                        override val extensions = null
                    },
                )
            },
        )
        respondJson(responses, HttpStatusCode.MultiStatus)
    }
}

fun MockEngineBuilder.mockAmtDeltakerPersonalia(gradering: PdlGradering) {
    post("/amt-deltaker/external/deltakere/personalia") { request ->
        val ids = request.decodeRequestBody<List<String>>()
        val personalia = ids.map { id ->
            DeltakerPersonaliaResponse(
                deltakerId = UUID.fromString(id),
                personident = "12345678901",
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
                navEnhetsnummer = "1206",
                erSkjermet = false,
                adressebeskyttelse = gradering,
            )
        }
        respondJson(personalia)
    }
}

fun MockEngineBuilder.mockKontoregisterOrganisasjon() {
    get(Regex(""".*/kontoregister/api/v1/hent-kontonummer-for-organisasjon/.*""")) {
        val kontonummer = KontonummerResponse(
            kontonr = "12345678901",
            mottaker = "Organisasjon",
        )
        respondJson(kontonummer)
    }
}
