package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject

fun Route.oppgaverRoutes() {
    val db: ApiDatabase by inject()
    val oppgaverService: OppgaverService by inject()
    route("oppgaver") {
        post {
            val userId = getNavIdent()
            val ansatt = db.session { queries.ansatt.getByNavIdent(userId) }
                ?: throw IllegalStateException("Fant ikke ansatt med navIdent $userId")

            val filter = call.receive<OppgaverFilter>()
            val tilsagnsOppgaver = oppgaverService.getOppgaverForTilsagn(filter, ansatt.roller)

            call.respond(tilsagnsOppgaver)
        }
    }
}

@Serializable
data class OppgaverFilter(
    val oppgavetyper: List<OppgaveType>,
    val tiltakstyper: List<Tiltakskode>,
    val regioner: List<String>,
)
