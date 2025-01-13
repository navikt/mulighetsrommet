package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.domain.Tiltakskode
import java.time.LocalDateTime
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
                    createdAt = LocalDateTime.now().minusDays(5),
                    frist = LocalDateTime.now().plusDays(7),
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
                    frist = LocalDateTime.now().plusDays(6),
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
                    frist = LocalDateTime.now().plusDays(5),
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
