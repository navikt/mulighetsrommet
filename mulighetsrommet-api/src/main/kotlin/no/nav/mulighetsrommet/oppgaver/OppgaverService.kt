package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.Returnert
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilAnnullering
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto.TilsagnStatus.TilGodkjenning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.*

class OppgaverService(val db: ApiDatabase) {
    fun oppgaver(filter: OppgaverFilter, roller: Set<NavAnsattRolle>): List<Oppgave> {
        val navEnheter = navEnheter(filter.regioner)

        return buildList {
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.TilsagnOppgaver.contains(it) }) {
                addAll(
                    tilsagnOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = navEnheter,
                        roller = roller,
                    ),
                )
            }
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.DelutbetalingOppgaver.contains(it) }) {
                addAll(
                    delutbetalingOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = navEnheter,
                        roller = roller,
                    ),
                )
            }
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.UtbetalingOppgaver.contains(it) }) {
                addAll(
                    utbetalingOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = navEnheter,
                        roller = roller,
                    ),
                )
            }
        }
    }

    fun tilsagnOppgaver(
        oppgavetyper: List<OppgaveType>,
        tiltakskoder: List<Tiltakskode>,
        kostnadssteder: List<String>,
        roller: Set<NavAnsattRolle>,
    ): List<Oppgave> {
        return db.session {
            queries.tilsagn
                .getAll(statuser = listOf(TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.TIL_ANNULLERING, TilsagnStatus.RETURNERT))
                .asSequence()
                .filter { oppgave ->
                    kostnadssteder.isEmpty() || oppgave.kostnadssted.enhetsnummer in kostnadssteder
                }
                .filter { tiltakskoder.isEmpty() || it.gjennomforing.tiltakskode in tiltakskoder }
                .mapNotNull { it.toOppgave() }
                .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
                .filter { it.type.rolle in roller }
                .toList()
        }
    }

    fun delutbetalingOppgaver(
        oppgavetyper: List<OppgaveType>,
        tiltakskoder: List<Tiltakskode>,
        kostnadssteder: List<String>,
        roller: Set<NavAnsattRolle>,
    ): List<Oppgave> {
        return db.session {
            queries.delutbetaling
                .getOppgaveData(
                    kostnadssteder = kostnadssteder.ifEmpty { null },
                    tiltakskoder = tiltakskoder.ifEmpty { null },
                )
                .mapNotNull { data ->
                    data.delutbetaling.toOppgave(data.tiltakskode, data.gjennomforingId)
                }
                .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
                .filter { it.type.rolle in roller }
        }
    }

    fun utbetalingOppgaver(
        oppgavetyper: List<OppgaveType>,
        tiltakskoder: List<Tiltakskode>,
        kostnadssteder: List<String>,
        roller: Set<NavAnsattRolle>,
    ): List<Oppgave> {
        return db.session {
            queries.utbetaling
                .getOppgaveData(
                    tiltakskoder = tiltakskoder.ifEmpty { null },
                )
                .filter {
                    if (kostnadssteder.isEmpty()) {
                        true
                    } else {
                        val tilsagn = db.session { queries.tilsagn.getTilsagnForGjennomforing(it.gjennomforing.id, it.periode) }
                        tilsagn.isEmpty() || tilsagn.any { it.kostnadssted.enhetsnummer in kostnadssteder }
                    }
                }
                .mapNotNull { it.toOppgave() }
                .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
                .filter { it.type.rolle in roller }
        }
    }

    private fun navEnheter(regioner: List<String>): List<String> {
        return regioner
            .flatMap { region ->
                db.session { queries.enhet.getAll(overordnetEnhet = region) }
            }
            .map { it.enhetsnummer }
    }

    private fun TilsagnDto.toOppgave(): Oppgave? = when (status) {
        is TilGodkjenning -> Oppgave(
            type = OppgaveType.TILSAGN_TIL_GODKJENNING,
            title = "Tilsagn til godkjenning",
            description = "Tilsagnet er til godkjenning og må behandles",
            tiltakstype = gjennomforing.tiltakskode,
            link = OppgaveLink(
                linkText = "Se tilsagn",
                link = "/gjennomforinger/${gjennomforing.id}/tilsagn/$id",
            ),
            createdAt = status.endretTidspunkt,
            deadline = periodeStart.atStartOfDay(),
        )
        is Returnert -> Oppgave(
            type = OppgaveType.TILSAGN_RETURNERT,
            title = "Tilsagn returnert",
            description = "Tilsagnet ble returnert av beslutter",
            tiltakstype = gjennomforing.tiltakskode,
            link = OppgaveLink(
                linkText = "Se tilsagn",
                link = "/gjennomforinger/${gjennomforing.id}/tilsagn/$id",
            ),
            createdAt = status.endretTidspunkt,
            deadline = periodeStart.atStartOfDay(),
        )
        is TilAnnullering -> Oppgave(
            type = OppgaveType.TILSAGN_TIL_ANNULLERING,
            title = "Tilsagn til annullering",
            description = "Tilsagnet er til annullering og må behandles",
            tiltakstype = gjennomforing.tiltakskode,
            link = OppgaveLink(
                linkText = "Se tilsagn",
                link = "/gjennomforinger/${gjennomforing.id}/tilsagn/$id",
            ),
            createdAt = status.endretTidspunkt,
            deadline = periodeStart.atStartOfDay(),
        )
        is TilsagnDto.TilsagnStatus.Annullert, TilsagnDto.TilsagnStatus.Godkjent -> null
    }

    private fun DelutbetalingDto.toOppgave(
        tiltakskode: Tiltakskode,
        gjennomforingId: UUID,
    ): Oppgave? = when (this) {
        is DelutbetalingDto.DelutbetalingTilGodkjenning -> Oppgave(
            type = OppgaveType.UTBETALING_TIL_GODKJENNING,
            title = "Utbetaling til godkjenning",
            description = "Utbetalingen er til godkjenning og må behandles",
            tiltakstype = tiltakskode,
            link = OppgaveLink(
                linkText = "Se utbetaling",
                link = "/gjennomforinger/$gjennomforingId/utbetalinger/$utbetalingId",
            ),
            createdAt = opprettetTidspunkt,
            deadline = opprettetTidspunkt.plusDays(7),
        )
        is DelutbetalingDto.DelutbetalingAvvist -> Oppgave(
            type = OppgaveType.UTBETALING_RETURNERT,
            title = "Utbetaling returnert",
            description = "Utbetaling ble returnert av beslutter",
            tiltakstype = tiltakskode,
            link = OppgaveLink(
                linkText = "Se utbetaling",
                link = "/gjennomforinger/$gjennomforingId/utbetalinger/$utbetalingId",
            ),
            createdAt = besluttetTidspunkt,
            deadline = besluttetTidspunkt.plusDays(2),
        )
        is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling,
        is DelutbetalingDto.DelutbetalingUtbetalt,
        -> null
    }

    private fun UtbetalingDto.toOppgave(): Oppgave? = when (status) {
        UtbetalingStatus.INNSENDT_AV_ARRANGOR -> Oppgave(
            type = OppgaveType.UTBETALING_TIL_BEHANDLING,
            title = "Utbetaling klar til behandling",
            description = "Innsendt utbetaling er klar til behandling",
            tiltakstype = tiltakstype.tiltakskode,
            link = OppgaveLink(
                linkText = "Se utbetaling",
                link = "/gjennomforinger/${gjennomforing.id}/utbetalinger/$id",
            ),
            createdAt = createdAt,
            deadline = createdAt.plusDays(7),
        )
        UtbetalingStatus.OPPRETTET_AV_NAV,
        UtbetalingStatus.KLAR_FOR_GODKJENNING,
        UtbetalingStatus.OVERFORT_TIL_UTBETALING,
        UtbetalingStatus.UTBETALT,
        -> null
    }
}
