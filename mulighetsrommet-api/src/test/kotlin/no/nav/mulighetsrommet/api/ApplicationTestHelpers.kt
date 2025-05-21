package no.nav.mulighetsrommet.api

import io.ktor.client.request.*
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

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
