package no.nav.mulighetsrommet.api.routes.featuretoggles

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.FeatureToggle
import no.nav.mulighetsrommet.unleash.FeatureToggleContext
import no.nav.mulighetsrommet.unleash.UnleashService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.featureTogglesRoute() {
    val unleashService: UnleashService by inject()

    route("/features") {
        get({
            tags = setOf("FeatureToggle")
            operationId = "getFeatureToggle"
            request {
                queryParameter<FeatureToggle>("feature") {
                    required = true
                }
                queryParameter<List<Tiltakskode>>("tiltakskoder")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Om feature er på eller ikke"
                    body<Boolean>()
                }
                default {
                    description = "En feil har oppstått"
                    body<ProblemDetail>()
                }
            }
        }) {
            val feature: FeatureToggle by call.parameters
            val tiltakskoder = call.parameters.getAll("tiltakskoder")
                ?.filter { it.isNotBlank() }
                ?.map { Tiltakskode.valueOf(it) }
                ?: emptyList()

            val context = FeatureToggleContext(
                userId = getNavIdent().value,
                sessionId = call.generateUnleashSessionId(),
                remoteAddress = call.request.origin.remoteAddress,
                tiltakskoder = tiltakskoder,
                orgnr = emptyList(),
            )

            val isEnabled = unleashService.isEnabled(feature, context)

            call.respond(isEnabled)
        }
    }
}

fun ApplicationCall.generateUnleashSessionId(): String {
    val uuid = UUID.randomUUID()
    val sessionId =
        java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
    val cookie = Cookie(name = "UNLEASH_SESSION_ID", value = sessionId, path = "/", maxAge = -1)
    this.response.cookies.append(cookie)
    return sessionId
}
