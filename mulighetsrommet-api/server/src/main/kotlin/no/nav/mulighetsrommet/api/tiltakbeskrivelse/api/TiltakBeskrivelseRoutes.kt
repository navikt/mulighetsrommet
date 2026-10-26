package no.nav.mulighetsrommet.api.tiltakbeskrivelse.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseHandling
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseKompaktDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.service.TiltakBeskrivelseAdminService
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.service.TiltakBeskrivelseRequest
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject
import java.util.UUID
import kotlin.collections.emptySet

@Serializable
data class TiltakBeskrivelsePublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class GetTiltakBeskrivelserRequest(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<Tiltakskode> = emptyList(),
    val publisert: Boolean? = null,
    val sort: String? = null,
    val visMineTiltakBeskrivelser: Boolean = false,
)

fun Route.tiltakBeskrivelseRoutes() {
    val db: AdminDatabase by inject()
    val service: TiltakBeskrivelseAdminService by inject()

    route("tiltak-beskrivelser") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("TiltakBeskrivelse")
                operationId = "upsertTiltakBeskrivelse"
                request {
                    body<TiltakBeskrivelseRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Individuell gjennomføring ble opprettet/oppdatert"
                        body<TiltakBeskrivelseDto>()
                    }
                    code(HttpStatusCode.BadRequest) {
                        description = "Valideringsfeil"
                        body<ValidationError>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<TiltakBeskrivelseRequest>()
                val navIdent = getNavIdent()
                val result = service.upsert(request, navIdent).mapLeft { ValidationError(errors = it) }
                call.respondWithStatusResponse(result)
            }

            put("{id}/tilgjengelig-for-veileder", {
                tags = setOf("TiltakBeskrivelse")
                operationId = "setPublisertTiltakBeskrivelse"
                request {
                    pathParameterUuid("id")
                    body<TiltakBeskrivelsePublisertRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilgjengelighet ble oppdatert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<TiltakBeskrivelsePublisertRequest>()
                val navIdent = getNavIdent()
                service.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }
        }

        post({
            tags = setOf("TiltakBeskrivelse")
            operationId = "getTiltakBeskrivelser"
            request {
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                body<GetTiltakBeskrivelserRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over tiltak-beskrivelser"
                    body<PaginatedResponse<TiltakBeskrivelseKompaktDto>>()
                }
            }
        }) {
            val pagination = getPaginationParams()
            val request = call.receive<GetTiltakBeskrivelserRequest>()
            val administratorNavIdent = request.visMineTiltakBeskrivelser.takeIf { it }?.let { getNavIdent() }

            val result = db.session {
                queries.tiltakBeskrivelse.getAllKompaktDto(
                    pagination = pagination,
                    navEnheter = request.navEnheter,
                    tiltakstyper = request.tiltakstyper,
                    publisert = request.publisert,
                    sortering = request.sort,
                    administratorNavIdent = administratorNavIdent,
                )
            }

            call.respond(PaginatedResponse.of(pagination, result.totalCount, result.items))
        }

        get("{id}", {
            tags = setOf("TiltakBeskrivelse")
            operationId = "getTiltakBeskrivelse"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Individuell gjennomføring"
                    body<TiltakBeskrivelseDto>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Ikke funnet"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val tiltakBeskrivelse = db.session { queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(id) }
                ?: call.respond(HttpStatusCode.NotFound)

            call.respond(tiltakBeskrivelse)
        }

        get("{id}/handlinger", {
            tags = setOf("TiltakBeskrivelse")
            operationId = "getTiltakBeskrivelseHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger for innlogget bruker"
                    body<Set<TiltakBeskrivelseHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            db.session { repository.navAnsatt.get(navIdent) }
                ?.let { call.respond(service.getHandlinger(it)) }
                ?: call.respond(emptySet<TiltakBeskrivelseHandling>())
        }
    }
}
