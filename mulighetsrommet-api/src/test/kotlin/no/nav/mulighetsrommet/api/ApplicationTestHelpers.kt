package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

fun MockOAuth2Server.createRequestWithAnsattClaims(
    ansatt: NavAnsattDbo,
    roles: Set<EntraGroupNavAnsattRolleMapping>,
): (HttpRequestBuilder) -> Unit = { request: HttpRequestBuilder ->
    val claims = getAnsattClaims(ansatt, roles)
    request.bearerAuth(issueToken(claims = claims).serialize())
}

fun getAnsattClaims(
    ansatt: NavAnsattDbo,
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
