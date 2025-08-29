package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilsagnRoutesGet() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()

    authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.BESLUTTER_TILSAGN)) {
        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            val result = db.session {
                val tilsagn = queries.tilsagn.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                val ansatt = queries.ansatt.getByNavIdent(navIdent)
                    ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

                val kostnadssted = tilsagn.kostnadssted.enhetsnummer

                val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT).let {
                    it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                }
                val annullering = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.ANNULLER)?.let {
                    it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                }
                val tilOppgjor = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.GJOR_OPP)?.let {
                    it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                }

                TilsagnDetaljerDto(
                    tilsagn = TilsagnDto.fromTilsagn(tilsagn),
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                )
            }

            call.respond(result)
        }
    }

    get("{id}/historikk") {
        val id = call.parameters.getOrFail<UUID>("id")
        val historikk = service.getEndringshistorikk(id)
        call.respond(historikk)
    }
}

private fun kanBeslutteTilsagn(
    totrinnskontroll: Totrinnskontroll,
    ansatt: NavAnsatt,
    kostnadssted: NavEnhetNummer,
): Boolean {
    return totrinnskontroll.behandletAv != ansatt.navIdent && ansatt.hasKontorspesifikkRolle(
        Rolle.BESLUTTER_TILSAGN,
        setOf(kostnadssted),
    )
}
