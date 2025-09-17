package no.nav.mulighetsrommet.api.arrangorflate.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.routes.featuretoggles.generateUnleashSessionId
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.FeatureToggle
import no.nav.mulighetsrommet.unleash.FeatureToggleContext
import no.nav.mulighetsrommet.unleash.UnleashService
import org.koin.ktor.ext.inject

fun Route.arrangorFeatureToggleRoutes() {
    val unleashService: UnleashService by inject()

    fun RoutingContext.arrangorTilganger(): List<Organisasjonsnummer>? {
        return call.principal<ArrangorflatePrincipal>()?.organisasjonsnummer
    }

    fun RoutingContext.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) = arrangorTilganger()
        ?.find { it == organisasjonsnummer }
        ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")

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
        requireTilgangHosArrangor(orgnr)

        val feature: FeatureToggle by call.parameters
        val tiltakskoder = call.parameters.getAll("tiltakskoder")
            ?.filter { it.isNotBlank() }
            ?.map { Tiltakskode.valueOf(it) }
            ?: emptyList()

        val context = FeatureToggleContext(
            userId = "",
            sessionId = call.generateUnleashSessionId(),
            remoteAddress = call.request.origin.remoteAddress,
            tiltakskoder = tiltakskoder,
            orgnr = listOf(orgnr),
        )

        call.respond(unleashService.isEnabled(feature, context))
    }
}
