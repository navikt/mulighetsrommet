package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilskuddBehandlingRoutes() {
    val service: TilskuddBehandlingService by inject()

    route("tilskudd-behandling") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            get("/{tilskuddBehandlingId}", {
                description = "Hent tilskuddsbehandling gitt id"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandling"
                request {
                    pathParameterUuid("tilskuddBehandlingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling"
                        body<TilskuddBehandlingDetaljerDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tilskuddBehandlingId = call.parameters.getOrFail<UUID>("tilskuddBehandlingId")
                val navIdent = getNavIdent()
                val result = service.getDetaljerDto(tilskuddBehandlingId, navIdent)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(result)
            }

            get({
                description = "Hent alle tilskuddsbehandlinger for en gjennomføring"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandlingerByGjennomforingId"
                request {
                    queryParameterUuid("gjennomforingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandlinger"
                        body<List<TilskuddBehandlingKompakt>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val gjennomforingId: UUID by call.queryParameters
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

                val result = service.upsert(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.BESLUTTER_TILSAGN) {
            post("/{id}/godkjenn", {
                tags = setOf("TilskuddBehandling")
                operationId = "godkjennTilskuddBehandling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling ble godkjent"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                val result = service.godkjenn(id, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            post("/{id}/returner", {
                tags = setOf("TilskuddBehandling")
                operationId = "returnerTilskuddBehandling"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<String>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling ble returnert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<AarsakerOgForklaringRequest<String>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .flatMap { service.returner(id, navIdent, it.aarsaker, it.forklaring) }
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}
