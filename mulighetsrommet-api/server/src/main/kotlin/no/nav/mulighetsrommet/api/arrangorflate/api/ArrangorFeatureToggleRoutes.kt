package no.nav.mulighetsrommet.api.arrangorflate.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.featuretoggle.api.generateUnleashSessionId
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject

fun Route.arrangorFeatureToggleRoutes() {
    val features: FeatureToggleService by inject()
    val altinnRettigheterService: AltinnRettigheterService by inject()

    get("/{orgnr}/features", {
        tags = setOf("FeatureToggle")
        operationId = "getFeatureToggle"
        request {
            pathParameter<Organisasjonsnummer>("orgnr")
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
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        requireTilgangHosArrangor(altinnRettigheterService, orgnr)

        val feature: FeatureToggle by call.parameters
        val tiltakskoder = call.parameters.getAll("tiltakskoder")
            ?.filter { it.isNotBlank() }
            ?.map { Tiltakskode.valueOf(it) }
            ?: emptyList()

        val context = FeatureToggleContext(
            sessionId = call.generateUnleashSessionId(),
            remoteAddress = call.request.origin.remoteAddress,
            tiltakskoder = tiltakskoder,
            orgnr = listOf(orgnr),
        )

        call.respond(features.isEnabled(feature, context))
    }
}
