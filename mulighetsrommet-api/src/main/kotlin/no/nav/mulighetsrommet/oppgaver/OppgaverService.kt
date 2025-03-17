package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingOppgaveData
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
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
                .getAll(
                    statuser = listOf(
                        TilsagnStatus.TIL_GODKJENNING,
                        TilsagnStatus.TIL_ANNULLERING,
                        TilsagnStatus.TIL_FRIGJORING,
                        TilsagnStatus.RETURNERT,
                    ),
                )
                .asSequence()
                .filter { oppgave ->
                    kostnadssteder.isEmpty() || oppgave.kostnadssted.enhetsnummer in kostnadssteder
                }
                .filter { tiltakskoder.isEmpty() || it.tiltakstype.tiltakskode in tiltakskoder }
                .mapNotNull { toOppgave(it) }
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
                .mapNotNull { toOppgave(it) }
                .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
                .filter { it.type.rolle in roller }
        }
    }

    fun utbetalingOppgaver(
        oppgavetyper: List<OppgaveType>,
        tiltakskoder: List<Tiltakskode>,
        kostnadssteder: List<String>,
        roller: Set<NavAnsattRolle>,
    ): List<Oppgave> = db.session {
        queries.utbetaling
            .getOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> utbetaling.innsender == UtbetalingDto.Innsender.ArrangorAnsatt }
            .filter { utbetaling -> queries.delutbetaling.getByUtbetalingId(utbetaling.id).isEmpty() }
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .map { toOppgave(it) }
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
            .filter { it.type.rolle in roller }
            .toList()
    }

    private fun QueryContext.byKostnadssted(
        utbetaling: UtbetalingDto,
        kostnadssteder: List<String>,
    ): Boolean = when {
        kostnadssteder.isEmpty() -> true
        else -> {
            queries.tilsagn
                .getAll(gjennomforingId = utbetaling.gjennomforing.id, periodeIntersectsWith = utbetaling.periode)
                .let { tilsagn -> tilsagn.isEmpty() || tilsagn.any { it.kostnadssted.enhetsnummer in kostnadssteder } }
        }
    }

    private fun navEnheter(regioner: List<String>): List<String> {
        return regioner
            .flatMap { region ->
                db.session { queries.enhet.getAll(overordnetEnhet = region) }
            }
            .map { it.enhetsnummer }
    }

    private fun QueryContext.toOppgave(tilsagn: TilsagnDto): Oppgave? {
        val tiltakstype = OppgaveTiltakstype(
            tiltakskode = tilsagn.tiltakstype.tiltakskode,
            navn = tilsagn.tiltakstype.navn,
        )

        val link = OppgaveLink(
            linkText = "Se tilsagn",
            link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
        )

        return when (tilsagn.status) {
            TilsagnStatus.TIL_GODKJENNING -> {
                val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
                Oppgave(
                    id = UUID.randomUUID(),
                    type = OppgaveType.TILSAGN_TIL_GODKJENNING,
                    title = "Tilsagn til godkjenning",
                    description = "Tilsagnet for ${tilsagn.gjennomforing.navn} er sendt til godkjenning",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = opprettelse.behandletTidspunkt,
                    oppgaveIcon = OppgaveIcon.TILSAGN,
                )
            }

            TilsagnStatus.RETURNERT -> {
                val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
                requireNotNull(opprettelse.besluttetTidspunkt)
                Oppgave(
                    id = UUID.randomUUID(),
                    type = OppgaveType.TILSAGN_RETURNERT,
                    title = "Tilsagn returnert",
                    description = "Tilsagnet for ${tilsagn.gjennomforing.navn} ble returnert av beslutter",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = opprettelse.besluttetTidspunkt,
                    oppgaveIcon = OppgaveIcon.TILSAGN,
                )
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
                Oppgave(
                    id = UUID.randomUUID(),
                    type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                    title = "Tilsagn til annullering",
                    description = "Tilsagnet for ${tilsagn.gjennomforing.navn} er sendt til annullering",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = annullering.behandletTidspunkt,
                    oppgaveIcon = OppgaveIcon.TILSAGN,
                )
            }

            TilsagnStatus.TIL_FRIGJORING -> {
                val frigjoring = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.FRIGJOR)
                Oppgave(
                    id = UUID.randomUUID(),
                    type = OppgaveType.TILSAGN_TIL_FRIGJORING,
                    title = "Tilsagn til frigjøring",
                    description = "Tilsagnet for ${tilsagn.gjennomforing.navn} er sendt til frigjøring",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = frigjoring.behandletTidspunkt,
                    oppgaveIcon = OppgaveIcon.TILSAGN,
                )
            }

            TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.FRIGJORT -> null
        }
    }

    private fun toOppgave(oppgavedata: DelutbetalingOppgaveData): Oppgave? {
        val (delutbetaling, gjennomforingId, gjennomforingsnavn, tiltakstype) = oppgavedata
        return when (delutbetaling) {
            is DelutbetalingDto.DelutbetalingTilGodkjenning -> Oppgave(
                id = UUID.randomUUID(),
                type = OppgaveType.UTBETALING_TIL_GODKJENNING,
                title = "Utbetaling til godkjenning",
                description = "Utbetalingen for $gjennomforingsnavn er sendt til godkjenning",
                tiltakstype = tiltakstype,
                link = OppgaveLink(
                    linkText = "Se utbetaling",
                    link = "/gjennomforinger/$gjennomforingId/utbetalinger/${delutbetaling.utbetalingId}",
                ),
                createdAt = delutbetaling.opprettelse.behandletTidspunkt,
                oppgaveIcon = OppgaveIcon.UTBETALING,
            )

            is DelutbetalingDto.DelutbetalingAvvist -> {
                Oppgave(
                    id = UUID.randomUUID(),
                    type = OppgaveType.UTBETALING_RETURNERT,
                    title = "Utbetaling returnert",
                    description = "Utbetaling for $gjennomforingsnavn ble returnert av beslutter",
                    tiltakstype = tiltakstype,
                    link = OppgaveLink(
                        linkText = "Se utbetaling",
                        link = "/gjennomforinger/$gjennomforingId/utbetalinger/${delutbetaling.utbetalingId}",
                    ),
                    createdAt = requireNotNull(delutbetaling.opprettelse.besluttetTidspunkt),
                    oppgaveIcon = OppgaveIcon.UTBETALING,
                )
            }

            is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling,
            is DelutbetalingDto.DelutbetalingUtbetalt,
            -> null
        }
    }

    private fun toOppgave(utbetaling: UtbetalingDto): Oppgave = Oppgave(
        id = UUID.randomUUID(),
        type = OppgaveType.UTBETALING_TIL_BEHANDLING,
        title = "Utbetaling klar til behandling",
        description = "Innsendt utbetaling for ${utbetaling.gjennomforing.navn} er klar til behandling",
        tiltakstype = OppgaveTiltakstype(
            tiltakskode = utbetaling.tiltakstype.tiltakskode,
            navn = utbetaling.tiltakstype.navn,
        ),
        link = OppgaveLink(
            linkText = "Se utbetaling",
            link = "/gjennomforinger/${utbetaling.gjennomforing.id}/utbetalinger/${utbetaling.id}",
        ),
        createdAt = utbetaling.createdAt,
        oppgaveIcon = OppgaveIcon.UTBETALING,
    )
}
