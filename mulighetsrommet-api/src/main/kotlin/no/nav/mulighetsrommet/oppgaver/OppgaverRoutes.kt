package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDateTime
import kotlin.text.get

fun Route.oppgaverRoutes() {
    route("oppgaver") {
        get {
            val userId = getNavIdent()
            val filter = getOppgaverFilter()

            val oppgaver = listOf(
                Oppgave(
                    type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                    title = "Tilsagn til beslutning",
                    description = "Tilsagn opprettet av Benny Beslutter er klar og venter beslutning",
                    tiltakstype = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    link = OppgaveLink(
                        linkText = "Gå til tilsagnet",
                        link = "https://nav.no/",
                    ),
                    createdAt = LocalDateTime.now().minusDays(5),
                    deadline = LocalDateTime.now().plusDays(7),
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
                    createdAt = LocalDateTime.now().minusDays(4),
                    deadline = LocalDateTime.now().plusDays(6),
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
                    createdAt = LocalDateTime.now().minusDays(3),
                    deadline = LocalDateTime.now().plusDays(5),
                ),
            )

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
