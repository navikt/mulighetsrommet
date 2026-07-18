package no.nav.mulighetsrommet.api.tiltakdokument.api

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
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentDto
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentHandling
import no.nav.mulighetsrommet.admin.tiltakdokument.service.TiltakDokumentAdminService
import no.nav.mulighetsrommet.admin.tiltakdokument.service.TiltakDokumentRequest
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject
import java.util.UUID
import kotlin.collections.emptySet

@Serializable
data class TiltakDokumentPublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class GetTiltakDokumenterRequest(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<Tiltakskode> = emptyList(),
)

fun Route.tiltakDokumentRoutes() {
    val db: AdminDatabase by inject()
    val service: TiltakDokumentAdminService by inject()

    route("tiltak-dokumenter") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("TiltakDokument")
                operationId = "upsertTiltakDokument"
                request {
                    body<TiltakDokumentRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Individuell gjennomføring ble opprettet/oppdatert"
                        body<TiltakDokumentDto>()
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
                val request = call.receive<TiltakDokumentRequest>()
                val result = service.upsert(request).mapLeft { ValidationError(errors = it) }
                call.respondWithStatusResponse(result)
            }

            put("{id}/tilgjengelig-for-veileder", {
                tags = setOf("TiltakDokument")
                operationId = "setPublisertTiltakDokument"
                request {
                    pathParameterUuid("id")
                    body<TiltakDokumentPublisertRequest>()
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
                val request = call.receive<TiltakDokumentPublisertRequest>()
                service.setPublisert(id, request.publisert)
                call.respond(HttpStatusCode.OK)
            }
        }

        post({
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokumenter"
            request {
                body<GetTiltakDokumenterRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over tiltak-dokumentøringer"
                    body<List<TiltakDokument>>()
                }
            }
        }) {
            val request = call.receive<GetTiltakDokumenterRequest>()

            val result = db.session {
                queries.tiltakDokument.getAllKompaktDto(
                    navEnheter = request.navEnheter,
                    tiltakstyper = request.tiltakstyper,
                )
            }

            call.respond(result)
        }

        get("{id}", {
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokument"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Individuell gjennomføring"
                    body<TiltakDokument>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Ikke funnet"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val tiltakDokument = db.session { queries.tiltakDokument.getTiltakDokumentDto(id) }
                ?: call.respond(HttpStatusCode.NotFound)

            call.respond(tiltakDokument)
        }

        get("{id}/handlinger", {
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokumentHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger for innlogget bruker"
                    body<Set<TiltakDokumentHandling>>()
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
                ?: call.respond(emptySet<TiltakDokumentHandling>())
        }
    }
}
