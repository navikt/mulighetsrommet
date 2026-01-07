package no.nav.mulighetsrommet.api.tilsagn.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilsagnRoutesBehandling() {
    val service: TilsagnService by inject()

    authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
        put({
            description = "Opprett tilsagn"
            tags = setOf("Tilsagn")
            operationId = "opprettTilsagn"
            request {
                body<TilsagnRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Opprettet tilsagn"
                    body<TilsagnDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<TilsagnRequest>()
            val navIdent = getNavIdent()

            val result = service.upsert(request, navIdent)
                .mapLeft { ValidationError(errors = it) }
                .map { TilsagnDto.fromTilsagn(it) }

            call.respondWithStatusResponse(result)
        }

        post("/{id}/til-annullering", {
            tags = setOf("Tilsagn")
            operationId = "tilAnnullering"
            request {
                pathParameterUuid("id")
                body<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsanget ble sendt til annullering"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            request.validate()
                .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                .onRight {
                    service.tilAnnulleringRequest(id, navIdent, it)
                    call.respond(HttpStatusCode.OK)
                }
        }

        post("/{id}/gjor-opp", {
            tags = setOf("Tilsagn")
            operationId = "gjorOpp"
            request {
                pathParameterUuid("id")
                body<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsanget ble sendt til oppgjør"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            request.validate()
                .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                .onRight {
                    service.tilGjorOppRequest(id, navIdent, it)
                    call.respond(HttpStatusCode.OK)
                }
        }

        delete("/{id}", {
            description = "Slett tilsagn"
            tags = setOf("Tilsagn")
            operationId = "slettTilsagn"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn ble slettet"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = service.slettTilsagn(id, navIdent)
                .mapLeft { ValidationError(errors = it) }
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }

    authorize(Rolle.BESLUTTER_TILSAGN) {
        post("/{id}/godkjenn", {
            tags = setOf("Tilsagn")
            operationId = "godkjennTilsagn"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn ble godkjent"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = service.godkjennTilsagn(id, navIdent)
                .mapLeft { ValidationError(errors = it) }
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }

        post("/{id}/returner", {
            tags = setOf("Tilsagn")
            operationId = "returnerTilsagn"
            request {
                pathParameterUuid("id")
                body<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn ble returnert"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            val navIdent = getNavIdent()

            val result = request.validate()
                .flatMap { service.returnerTilsagn(id, navIdent, it.aarsaker, it.forklaring) }
                .mapLeft { ValidationError(errors = it) }
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }
}
