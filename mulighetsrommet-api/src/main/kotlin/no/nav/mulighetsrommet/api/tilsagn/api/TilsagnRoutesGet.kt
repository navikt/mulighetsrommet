package no.nav.mulighetsrommet.api.tilsagn.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.http.content.default
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.tilsagnHandlinger
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilsagnRoutesGet() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()

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

                val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT).toDto()
                val annullering = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.ANNULLER)?.toDto()
                val tilOppgjor = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.GJOR_OPP)?.toDto()

                TilsagnDetaljerDto(
                    tilsagn = TilsagnDto.fromTilsagn(tilsagn),
                    beregning = TilsagnBeregningDto.from(tilsagn.beregning),
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                    handlinger = tilsagnHandlinger(
                        id = tilsagn.id,
                        kostnadssted = tilsagn.kostnadssted.enhetsnummer,
                        status = tilsagn.status,
                        belopBrukt = tilsagn.belopBrukt,
                        ansatt = ansatt,
                    ),
                )
            }

            call.respond(result)
        }
    }

    get("{id}/historikk", {
        description = "Hent endringshistorikk for tilsagn"
        tags = setOf("Tilsagn")
        operationId = "getTilsagnEndringshistorikk"
        request {
            pathParameterUuid("id")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Endringshistorikk for tilsagn"
                body<EndringshistorikkDto>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val id = call.parameters.getOrFail<UUID>("id")
        val historikk = service.getEndringshistorikk(id)
        call.respond(historikk)
    }
}
