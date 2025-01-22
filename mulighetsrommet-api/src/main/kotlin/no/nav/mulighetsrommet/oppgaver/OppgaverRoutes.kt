package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.domain.Tiltakskode

fun Route.oppgaverRoutes() {
    route("oppgaver") {
        get {
            val userId = getNavIdent()
            val filter = getOppgaverFilter()

            val oppgaver = emptyList<Oppgave>()

            val sortedOppgaver = oppgaver.filter { oppgave ->
                val matcherOppgaveType = filter.oppgavetyper.isEmpty() || filter.oppgavetyper.contains(oppgave.type)
                val matcherTiltakstype = filter.tiltakstyper.isEmpty() || filter.tiltakstyper.contains(oppgave.tiltakstype)

                matcherOppgaveType && matcherTiltakstype
            }

            call.respond(sortedOppgaver)
        }
    }
}

fun RoutingContext.getOppgaverFilter(): OppgaverFilter {
    val oppgavetyper = call.parameters.getAll("oppgavetyper") ?: emptyList()
    val tiltakstyper = call.parameters.getAll("tiltakstyper") ?: emptyList()

    return OppgaverFilter(
        oppgavetyper = oppgavetyper.map { OppgaveType.valueOf(it) },
        tiltakstyper = tiltakstyper.map { Tiltakskode.valueOf(it) },
    )
}

data class OppgaverFilter(
    val oppgavetyper: List<OppgaveType>,
    val tiltakstyper: List<Tiltakskode>,
)
