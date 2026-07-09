package no.nav.mulighetsrommet.api.tilsagn.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilsagnRoutesGet() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val personaliaService: PersonaliaService by inject()

    authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.BESLUTTER_TILSAGN)) {
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

            val result = db.session {
                val tilsagn = queries.tilsagn.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                val ansatt = queries.ansatt.getByNavIdent(navIdent)
                    ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

                val opprettelse = queries.totrinnskontroll.getDtoOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
                val annullering = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_ANNULLERING)
                val tilOppgjor = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_OPPGJOR)

                val personalia = personaliaService.getPersonalia(
                    tilsagn.deltakere.map { it.deltakerId },
                    PersonaliaService.OnBehalfOf.NavAnsatt(call.getAccessType().requireAzureAd()),
                )
                val deltakere = tilsagn.deltakere.map {
                    TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                }
                TilsagnDetaljerDto(
                    tilsagn = TilsagnDto.from(tilsagn),
                    beregning = TilsagnBeregningDto.from(tilsagn.beregning),
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                    handlinger = service.handlinger(tilsagn, ansatt),
                    deltakere = deltakere,
                )
            }

            call.respond(result)
        }
    }
}
