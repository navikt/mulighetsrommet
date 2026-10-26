package no.nav.mulighetsrommet.api.tilsagn.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnDtoQuery
import no.nav.mulighetsrommet.api.tilsagn.TilsagnDtoQueryError
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilsagnRoutesGet() {
    val tilsagnDtoQuery: TilsagnDtoQuery by inject()

    authorize(
        anyOf = setOf(
            Rolle.OKONOMI_LES,
            Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK,
            Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK,
            Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
            Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
        ),
    ) {
        get("{id}", {
            description = "Hent tilsagn"
            tags = setOf("Tilsagn")
            operationId = "getTilsagn"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Detaljer om tilsagn"
                    body<TilsagnDetaljerDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()
            val onBehalfOf = PersonaliaService.OnBehalfOf.NavAnsatt(call.getAccessType().requireAzureAd())

            val result = tilsagnDtoQuery
                .getDetaljer(id, navIdent, onBehalfOf)
                .mapLeft { toProblemDetail(it) }

            call.respondWithStatusResponse(result)
        }
    }
}

private fun toProblemDetail(error: TilsagnDtoQueryError): ProblemDetail = when (error) {
    is TilsagnDtoQueryError.IkkeFunnet -> NotFound(error.message)
    is TilsagnDtoQueryError.ManglerTilgang -> Forbidden(error.message)
}
