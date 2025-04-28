package no.nav.mulighetsrommet.api.routes.featuretoggles

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.FeatureToggleContext
import no.nav.mulighetsrommet.unleash.UnleashService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.featureTogglesRoute() {
    val unleashService: UnleashService by inject()

    route("/features") {
        get {
            val feature: String by call.parameters
            val tiltakskoder = call.parameters.getAll("tiltakskoder")
                ?.map { Tiltakskode.valueOf(it) }
                ?: emptyList()

            val context = FeatureToggleContext(
                userId = getNavIdent().value,
                sessionId = call.generateSessionId(),
                remoteAddress = call.request.origin.remoteAddress,
                tiltakskoder = tiltakskoder,
                orgnr = emptyList(),
            )

            val isEnabled = unleashService.isEnabled(feature, context)

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
