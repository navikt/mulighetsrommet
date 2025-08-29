package no.nav.mulighetsrommet.api.tilsagn.api

import arrow.core.flatMap
import arrow.core.right
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttTotrinnskontrollRequest
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilsagnRoutesBehandling() {
    val service: TilsagnService by inject()

    authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
        put {
            val request = call.receive<TilsagnRequest>()
            val navIdent = getNavIdent()

            val result = service.upsert(request, navIdent)
                .mapLeft { ValidationError(errors = it) }

            call.respondWithStatusResponse(result)
        }

        post("/{id}/til-annullering") {
            val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                .onRight {
                    service.tilAnnulleringRequest(id, navIdent, request)
                    call.respond(HttpStatusCode.OK)
                }
        }

        post("/{id}/gjor-opp") {
            val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                .onRight {
                    service.tilGjorOppRequest(id, navIdent, request)
                    call.respond(HttpStatusCode.OK)
                }
        }

        delete("/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = service.slettTilsagn(id, navIdent)
                .mapLeft { ValidationError(errors = it) }
                .map { HttpStatusCode.OK }

            call.respondWithStatusResponse(result)
        }
    }

    authorize(Rolle.BESLUTTER_TILSAGN) {
        post("/{id}/beslutt") {
            val id = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>>()
            val navIdent = getNavIdent()

            when (request.besluttelse) {
                Besluttelse.GODKJENT -> Unit.right()
                Besluttelse.AVVIST -> validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
            }
                .flatMap {
                    service.beslutt(id, request, navIdent)
                }
                .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                .onRight { call.respond(HttpStatusCode.OK) }
        }
    }
}
