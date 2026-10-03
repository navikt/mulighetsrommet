package no.nav.mulighetsrommet.api.tilsagn.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilsagnRoutesBehandling() {
    val service: TilsagnService by inject()

    authorize(
        anyOf = setOf(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
    ) {
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
                .mapLeft(::toProblemDetail)
                .map { TilsagnDto.from(it) }

            call.respondWithStatusResponse(result)
        }

        post("/{id}/til-annullering", {
            tags = setOf("Tilsagn")
            operationId = "tilAnnullering"
            request {
                pathParameterUuid("id")
                body<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
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
            val request = call.receive<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = request.validate()
                .mapLeft(TilsagnError::Valideringsfeil)
                .flatMap { service.tilAnnulleringRequest(id, navIdent, it) }
                .mapLeft(::toProblemDetail)
                .map { HttpStatusCode.OK }
            call.respondWithStatusResponse(result)
        }

        post("/{id}/gjor-opp", {
            tags = setOf("Tilsagn")
            operationId = "gjorOpp"
            request {
                pathParameterUuid("id")
                body<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
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
            val request = call.receive<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = request.validate()
                .mapLeft(TilsagnError::Valideringsfeil)
                .flatMap { service.tilOppgjorRequest(id, navIdent, it) }
                .mapLeft(::toProblemDetail)
                .map { HttpStatusCode.OK }
            call.respondWithStatusResponse(result)
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
                .mapLeft(::toProblemDetail)
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }

    authorize(
        anyOf = setOf(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
    ) {
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
                .mapLeft(::toProblemDetail)
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }

    authorize(
        anyOf = setOf(
            Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK,
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
            Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
        ),
    ) {
        post("/{id}/returner", {
            tags = setOf("Tilsagn")
            operationId = "returnerTilsagn"
            request {
                pathParameterUuid("id")
                body<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
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
            val request = call.receive<AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>>()
            val navIdent = getNavIdent()

            val result = request.validate()
                .mapLeft(TilsagnError::Valideringsfeil)
                .flatMap { service.returnerTilsagn(id, navIdent, it.aarsaker, it.begrunnelse) }
                .mapLeft(::toProblemDetail)
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }
}

private fun toProblemDetail(error: TilsagnError): ProblemDetail = when (error) {
    is TilsagnError.ManglerTilgang -> Forbidden(error.message)
    is TilsagnError.Valideringsfeil -> ValidationError("Valideringsfeil", error.errors)
}
