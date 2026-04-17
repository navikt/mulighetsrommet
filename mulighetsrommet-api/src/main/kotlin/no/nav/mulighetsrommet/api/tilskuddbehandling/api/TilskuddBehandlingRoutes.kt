package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilskuddBehandlingRoutes() {
    val service: TilskuddBehandlingService by inject()

    route("tilskudd-behandling") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            get("/{gjennomforingId}", {
                description = "Hent alle tilskuddsbehandlinger for en gjennomføring"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandlingerByGjennomforingId"
                request {
                    pathParameterUuid("gjennomforingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandlinger"
                        body<List<TilskuddBehandlingDto>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val gjennomforingId = call.parameters.getOrFail<UUID>("gjennomforingId")
                val result = service.getByGjennomforingId(gjennomforingId)
                call.respond(result)
            }

            post({
                description = "Opprett tilskudd behandling"
                tags = setOf("TilskuddBehandling")
                operationId = "opprettTilskuddBehandling"
                request {
                    body<TilskuddBehandlingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskudd behandling opprettet"
                        body<TilskuddBehandlingDto>()
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
                val request = call.receive<TilskuddBehandlingRequest>()
                val navIdent = getNavIdent()

                val result = service.opprett(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }
        }
    }
}
