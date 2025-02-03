package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.Returnert
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilAnnullering
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilGodkjenning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2

class OppgaverService(val db: ApiDatabase) {

    fun getOppgaverForTilsagn(filter: OppgaverFilter, ansattRoller: Set<NavAnsattRolle>): List<Oppgave> {
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
                when {
                    oppgavetype == OppgaveType.TILSAGN_TIL_GODKJENNING && ansattRoller.contains(NavAnsattRolle.OKONOMI_BESLUTTER) -> listOf(
                        TilsagnStatus.TIL_GODKJENNING,
                    )

                    oppgavetype == OppgaveType.TILSAGN_TIL_ANNULLERING && ansattRoller.contains(NavAnsattRolle.OKONOMI_BESLUTTER) -> listOf(
                        TilsagnStatus.TIL_ANNULLERING,
                    )

                    oppgavetype == OppgaveType.TILSAGN_RETURNERT_AV_BESLUTTER && ansattRoller.contains(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV) -> listOf(
                        TilsagnStatus.RETURNERT,
                    )

                    else -> null
                }
            }.flatten()

            val enheter = db.session { queries.enhet.getAll(statuser = listOf(NavEnhetStatus.AKTIV, NavEnhetStatus.UNDER_ETABLERING)) }
            val enheterToLokalkontor = enheter
                .groupBy { it.overordnetEnhet }
                .mapValues { it.value.map { enhet -> enhet.enhetsnummer } }
                .filterKeys { key -> filter.regioner.isEmpty() || filter.regioner.contains(key) }

            db.session {
                val oppgaver = queries.tilsagn
                    .getAll(
                        statuser = tilsagnStatuser,
                    )
                    .filter { oppgave ->
                        if (filter.regioner.isEmpty()) {
                            return@filter true
                        }
                        enheterToLokalkontor.any { (_, underenheter) ->
                            underenheter.contains(oppgave.kostnadssted.enhetsnummer)
                        }
                    }
                    .filter { tiltakskoder.contains(it.gjennomforing.tiltakskode) }
                    .map {
                        Oppgave(
                            type = it.status.toType(),
                            title = it.status.toTitle(),
                            description = it.status.toDescription(),
                            tiltakstype = it.gjennomforing.tiltakskode,
                            link = OppgaveLink(
                                linkText = "Se tilsagn",
                                link = "/gjennomforinger/${it.gjennomforing.id}/tilsagn/${it.id}",
                            ),
                            createdAt = it.status.createdAt(),
                            deadline = it.periodeStart.atStartOfDay(),
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

    private fun TilsagnDto.TilsagnStatus.toDescription(): String {
        return when (this) {
            is TilGodkjenning -> "Tilsagnet er til godkjenning og må behandles"
            is Returnert -> "Tilsagnet ble returnert av beslutter"
            is TilAnnullering -> "Tilsagnet er til annullering og må behandles"
            else -> {
                throw IllegalStateException("Ukjent tilsagnstatus")
            }
        }
    }

    private fun TilsagnDto.TilsagnStatus.toType(): OppgaveType {
        return when (this) {
            is TilGodkjenning -> OppgaveType.TILSAGN_TIL_GODKJENNING
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
}
