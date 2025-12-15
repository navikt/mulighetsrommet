package no.nav.mulighetsrommet.api.tiltakstype

import io.github.smiley4.ktoropenapi.config.descriptors.empty
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.featuretoggle.api.generateUnleashSessionId
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()
    val veilederflateService: VeilederflateService by inject()
    val featureToggleService: FeatureToggleService by inject()

    route("tiltakstyper") {
        get({
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstyper"
            request {
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltakstyper i Tiltaksadministrasjon"
                    body<List<TiltakstypeDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val filter = getTiltakstypeFilter()

            val opplaeringTiltakEnabled = featureToggleService.isEnabled(
                feature = FeatureToggle.TILTAKSKODER_OPPLAERING_2025,
                context = FeatureToggleContext(
                    userId = getNavIdent().toString(),
                    sessionId = call.generateUnleashSessionId(),
                    remoteAddress = call.request.origin.remoteAddress,
                    tiltakskoder = emptyList(),
                    orgnr = emptyList(),
                )
            )

            val tiltakstyper = tiltakstypeService.getAllGruppetiltak(filter, opplaeringTiltakEnabled)

            call.respond(tiltakstyper)
        }

        get("{id}", {
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstype"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltakstype"
                    body<TiltakstypeDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            call.respond(tiltakstype)
        }

        get("{id}/faneinnhold", {
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstypeFaneinnhold"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Faneinnhold for tiltakstype"
                    body<VeilederflateTiltakstype>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            val veilederflateTiltakstype: VeilederflateTiltakstype = veilederflateService.hentTiltakstyper()
                .find { UUID.fromString(it.sanityId) == tiltakstype.sanityId }
                ?: return@get call.respondText(
                    "Det finnes ikke noe faneinnhold for tiltakstype med id $id",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(veilederflateTiltakstype)
        }
    }
}

data class TiltakstypeFilter(
    val sortering: String? = null,
)

fun RoutingContext.getTiltakstypeFilter(): TiltakstypeFilter {
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        sortering = sortering,
    )
}
