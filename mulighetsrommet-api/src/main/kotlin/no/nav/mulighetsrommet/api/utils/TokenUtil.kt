package no.nav.mulighetsrommet.api.utils

import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun ApplicationCall.getAccessToken(): String? = request.parseAuthorizationHeader()?.getBlob()

private fun HttpAuthHeader.getBlob(): String? = when {
    this is HttpAuthHeader.Single && authScheme.lowercase() in listOf("bearer") -> blob
    else -> null
}
