package no.nav.mulighetsrommet.api.routes.featuretoggles

import io.getunleash.UnleashContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.unleash.UnleashService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.featureTogglesRoute() {
    route("/api/v1/internal/features") {
        val unleashService: UnleashService by inject()
        get {
            val feature: String = call.request.queryParameters.getOrFail("feature")
            val isEnabled = unleashService.get()
                .isEnabled(
                    feature,
                    UnleashContext(
                        getNavIdent(),
                        call.generateSessionId(),
                        call.request.origin.remoteAddress,
                        emptyMap(),
                    ),
                )
            call.respond(isEnabled)
        }
    }
}

private fun ApplicationCall.generateSessionId(): String {
    val uuid = UUID.randomUUID()
    val sessionId =
        java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
    val cookie = Cookie(name = "UNLEASH_SESSION_ID", value = sessionId, path = "/", maxAge = -1)
    this.response.cookies.append(cookie)
    return sessionId
}
