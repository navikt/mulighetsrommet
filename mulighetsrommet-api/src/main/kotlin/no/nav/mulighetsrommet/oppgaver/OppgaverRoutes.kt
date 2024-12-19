package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.domain.Tiltakskode
import kotlin.text.get

fun Route.oppgaverRoutes() {
    route("oppgaver") {
        get {
            val userId = getNavIdent()
            val filter = getOppgaverFilter()

            val oppgaver = listOf(
                Oppgave(
                    type = OppgaveType.TILSAGN_TIL_BESLUTNING,
                    title = "Tilsagn til beslutning",
                    description = "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
                    tiltakstype = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    link = OppgaveLink(
                        linkText = "Gå til tilsagnet",
                        link = "https://nav.no/",
                    ),
                ),
                Oppgave(
                    type = OppgaveType.TILSAGN_TIL_BESLUTNING,
                    title = "Send tilsagn til beslutning",
                    description = "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
                    tiltakstype = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    link = OppgaveLink(
                        linkText = "Gå til tilsagnet",
                        link = "https://nav.no/",
                    ),
                ),
            )

            call.respond(oppgaver)
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
