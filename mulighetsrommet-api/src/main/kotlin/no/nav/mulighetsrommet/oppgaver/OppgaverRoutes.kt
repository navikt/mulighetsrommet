package no.nav.mulighetsrommet.oppgaver

import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.Returnert
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilAnnullering
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilGodkjenning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject
import java.time.LocalDateTime

fun Route.oppgaverRoutes() {
    val db: ApiDatabase by inject()
    route("oppgaver") {
        post {
            val userId = getNavIdent()
            val ansatt = db.session { queries.ansatt.getByNavIdent(userId) }
                ?: throw IllegalStateException("Fant ikke ansatt med navIdent $userId")

            val filter = call.receive<OppgaverFilter>()
            val tilsagnsOppgaver = getOppgaverForTilsagn(filter, db)

            call.respond(tilsagnsOppgaver)
        }
    }
}

private fun getOppgaverForTilsagn(filter: OppgaverFilter, db: ApiDatabase): List<Oppgave> {
    val oppgaver = buildList<Oppgave> {
        val oppgavetyper = if (filter.oppgavetyper.isEmpty()) {
            OppgaveType.entries
        } else {
            filter.oppgavetyper
        }

        val tiltakskoder = if (filter.tiltakstyper.isEmpty()) {
            db.session { queries.tiltakstype.getAll().map { it.tiltakskode } }
        } else {
            filter.tiltakstyper
        }

        val tilsagnStatuser = oppgavetyper.mapNotNull { oppgavetype ->
            when (oppgavetype) {
                OppgaveType.TILSAGN_TIL_BESLUTNING -> listOf(
                    TilsagnStatus.TIL_GODKJENNING,
                )

                OppgaveType.TILSAGN_TIL_ANNULLERING -> listOf(TilsagnStatus.TIL_ANNULLERING)

                OppgaveType.TILSAGN_RETURNERT_AV_BESLUTTER -> listOf(TilsagnStatus.RETURNERT)

                else -> null
            }
        }.flatten()

        // TODO Finne ut hvordan vi bare returnerer korrekte oppgaver basert på roller til innlogget bruker

        db.session {
            val oppgaver = queries.tilsagn.getAll(statuser = tilsagnStatuser)
                .filter { tiltakskoder.contains(it.gjennomforing.tiltakskode) }.map {
                    Oppgave(
                        type = it.status.toType(),
                        title = it.status.toTitle(),
                        description = "Tilsagnet trenger behandling",
                        tiltakstype = it.gjennomforing.tiltakskode,
                        link = OppgaveLink(
                            linkText = "Se tilsagn",
                            link = "/gjennomforinger/${it.gjennomforing.id}/tilsagn/${it.id}",
                        ),
                        createdAt = it.status.createdAt(),
                        deadline = it.periodeSlutt.atStartOfDay(),
                    )
                }

            addAll(oppgaver)
        }
    }

    return oppgaver
}

private fun TilsagnDto.TilsagnStatus.toTitle(): String {
    return when (this) {
        is TilGodkjenning -> "Tilsagn til godkjenning"
        is Returnert -> "Tilsagn returnert"
        is TilAnnullering -> "Tilsagn til annullering"
        else -> {
            throw IllegalStateException("Ukjent tilsagnstatus")
        }
    }
}

private fun TilsagnDto.TilsagnStatus.toType(): OppgaveType {
    return when (this) {
        is TilGodkjenning -> OppgaveType.TILSAGN_TIL_BESLUTNING
        is Returnert -> OppgaveType.TILSAGN_RETURNERT_AV_BESLUTTER
        is TilAnnullering -> OppgaveType.TILSAGN_TIL_ANNULLERING
        else -> throw IllegalStateException("Ukjent tilsagnstatus")
    }
}

private fun TilsagnDto.TilsagnStatus.createdAt(): LocalDateTime {
    return when (this) {
        is TilGodkjenning -> this.endretTidspunkt
        is Returnert -> this.endretTidspunkt
        is TilAnnullering -> this.endretTidspunkt
        else -> throw IllegalStateException("Ukjent tilsagnstatus")
    }
}

@Serializable
data class OppgaverFilter(
    val oppgavetyper: List<OppgaveType>,
    val tiltakstyper: List<Tiltakskode>,
)
