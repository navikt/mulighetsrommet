package no.nav.mulighetsrommet.api

import io.ktor.client.request.*
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.createRequestWithAnsattClaims(
    ansatt: NavAnsattDbo,
    roles: Set<AdGruppeNavAnsattRolleMapping>,
): (HttpRequestBuilder) -> Unit = { request: HttpRequestBuilder ->
    val claims = getAnsattClaims(ansatt, roles)
    request.bearerAuth(issueToken(claims = claims).serialize())
}

fun getAnsattClaims(
    ansatt: NavAnsattDbo,
    roles: Set<AdGruppeNavAnsattRolleMapping>,
): Map<String, Any> {
    return mapOf(
        "NAVident" to ansatt.navIdent.value,
        "oid" to ansatt.azureId,
        "sid" to UUID.randomUUID().toString(),
        "groups" to roles.map { it.adGruppeId.toString() },
    )
}
