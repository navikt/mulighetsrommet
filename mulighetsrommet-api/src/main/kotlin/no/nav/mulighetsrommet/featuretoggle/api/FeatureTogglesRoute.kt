package no.nav.mulighetsrommet.featuretoggle.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject
import java.lang.Long
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.setOf
import kotlin.getValue
import kotlin.text.isNotBlank

fun Route.featureTogglesRoute() {
    val features: FeatureToggleService by inject()

    route("/features") {
        get({
            tags = setOf("FeatureToggle")
            operationId = "getFeatureToggle"
            request {
                queryParameter<FeatureToggle>("feature") {
                    required = true
                }
                queryParameter<List<Tiltakskode>>("tiltakskoder") {
                    explode = true
                }
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

            val isEnabled = features.isEnabled(feature, context)

            call.respond(isEnabled)
        }
    }
}

fun ApplicationCall.generateUnleashSessionId(): String {
    val uuid = UUID.randomUUID()
    val sessionId =
        Long.toHexString(uuid.mostSignificantBits) + Long.toHexString(uuid.leastSignificantBits)
    val cookie = Cookie(name = "UNLEASH_SESSION_ID", value = sessionId, path = "/", maxAge = -1)
    this.response.cookies.append(cookie)
    return sessionId
}
