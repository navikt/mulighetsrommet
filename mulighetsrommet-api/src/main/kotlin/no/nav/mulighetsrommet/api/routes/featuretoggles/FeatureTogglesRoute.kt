package no.nav.mulighetsrommet.api.routes.featuretoggles

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.veilederflate.routes.NavVeilederDto
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.FeatureToggleContext
import no.nav.mulighetsrommet.unleash.UnleashService
import org.koin.ktor.ext.inject
import java.util.*

enum class FeatureToggle(val key: String) {
    AVBRYT_UTBETALING("mulighetsrommet.migrering.okonomi.avbryt-utbetaling"),
    MIGRERING_TILSAGN("mulighetsrommet.tiltakstype.migrering.tilsagn"),
    MIGRERING_UTBETALING("mulighetsrommet.tiltakstype.migrering.okonomi"),
    ARRANGORFLATE_OPPRETT_UTBETEALING_INVESTERINGER("arrangorflate.utbetaling.opprett-utbetaling-knapp"),
    ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS("arrangorflate.utbetaling.opprett-utbetaling.annen-avtalt-ppris"),
}

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
                    description = "Informasjon om veileder"
                    body<NavVeilederDto>()
                }
                default {
                    description = "En feil har oppstått"
                    body<ProblemDetail>()
                }
            }
        }) {
            val feature: FeatureToggle by call.parameters
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

            val isEnabled = unleashService.isEnabled(feature.key, context)

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
