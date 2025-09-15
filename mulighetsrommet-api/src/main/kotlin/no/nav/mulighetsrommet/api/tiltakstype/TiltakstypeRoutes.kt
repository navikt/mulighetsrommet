package no.nav.mulighetsrommet.api.tiltakstype

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()
    val veilederflateService: VeilederflateService by inject()

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

            val tiltakstyper = tiltakstypeService.getAll(filter)

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
