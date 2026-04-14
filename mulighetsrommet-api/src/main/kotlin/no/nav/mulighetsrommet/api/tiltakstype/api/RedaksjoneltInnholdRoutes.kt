package no.nav.mulighetsrommet.api.tiltakstype.api

import arrow.core.nel
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.service.RedaksjoneltInnholdLenkeService
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.redaksjoneltInnholdRoutes() {
    val redaksjoneltInnholdLenkeService: RedaksjoneltInnholdLenkeService by inject()

    route("redaksjonelt-innhold/lenker") {
        get({
            tags = setOf("RedaksjoneltInnhold")
            operationId = "getLenker"
            response {
                code(HttpStatusCode.OK) {
                    body<List<RedaksjoneltInnholdLenke>>()
                }
            }
        }) {
            call.respond(redaksjoneltInnholdLenkeService.getAll())
        }

        authorize(Rolle.TILTAKSTYPER_SKRIV) {
            put("{id}", {
                tags = setOf("RedaksjoneltInnhold")
                operationId = "upsertLenke"
                request {
                    pathParameterUuid("id")
                    body<RedaksjoneltInnholdLenkeRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        body<RedaksjoneltInnholdLenke>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<RedaksjoneltInnholdLenkeRequest>()
                val lenke = RedaksjoneltInnholdLenke(
                    id = id,
                    url = request.url,
                    navn = request.navn,
                    beskrivelse = request.beskrivelse,
                )
                call.respond(redaksjoneltInnholdLenkeService.upsert(lenke))
            }

            delete("{id}", {
                tags = setOf("RedaksjoneltInnhold")
                operationId = "deleteLenke"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.NoContent) {
                        description = "Lenke ble slettet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters

                if (!redaksjoneltInnholdLenkeService.delete(id)) {
                    val error = ValidationError(
                        detail = "Lenke kan ikke slettes",
                        errors = FieldError.of("Lenke kan ikke slettes").nel(),
                    )
                    return@delete call.respondWithProblemDetail(error)
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

@Serializable
data class RedaksjoneltInnholdLenkeRequest(
    val url: String,
    val navn: String?,
    val beskrivelse: String?,
)
