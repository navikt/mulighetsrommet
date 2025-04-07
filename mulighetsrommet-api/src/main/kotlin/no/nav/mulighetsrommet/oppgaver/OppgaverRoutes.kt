package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject

fun Route.oppgaverRoutes() {
    val db: ApiDatabase by inject()
    val service: OppgaverService by inject()

    route("oppgaver") {
        post {
            val userId = getNavIdent()
            val ansatt = db.session { queries.ansatt.getByNavIdent(userId) }
                ?: throw IllegalStateException("Fant ikke ansatt med navIdent $userId")

            val filter = call.receive<OppgaverFilter>()

            val oppgaver = service.oppgaver(
                filter = filter,
                ansatt = ansatt.navIdent,
                roller = ansatt.roller,
            )

            call.respond(oppgaver)
        }
    }
}

@Serializable
data class OppgaverFilter(
    val oppgavetyper: Set<OppgaveType>,
    val tiltakskoder: Set<Tiltakskode>,
    val regioner: Set<NavEnhetNummer>,
)
