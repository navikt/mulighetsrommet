package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingOppgaveData
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode

class OppgaverService(val db: ApiDatabase) {
    fun oppgaver(filter: OppgaverFilter, ansatt: NavIdent, roller: Set<NavAnsattRolle>): List<Oppgave> {
        val ansattesRoller = roller.map { it.rolle }.toSet()

        // TODO: kostnadssteder basert på ansattes roller (Dette må gjøres per rolle/oppgavetype)
        val kostnadssteder = getNavEnheterForRegioner(filter.regioner)

        return buildList {
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.TilsagnOppgaver.contains(it) }) {
                addAll(
                    tilsagnOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = kostnadssteder,
                        roller = ansattesRoller,
                        ansatt = ansatt,
                    ),
                )
            }
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.DelutbetalingOppgaver.contains(it) }) {
                addAll(
                    delutbetalingOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = kostnadssteder,
                        ansatt = ansatt,
                        roller = ansattesRoller,
                    ),
                )
            }
            if (filter.oppgavetyper.isEmpty() || filter.oppgavetyper.any { OppgaveType.UtbetalingOppgaver.contains(it) }) {
                addAll(
                    utbetalingOppgaver(
                        tiltakskoder = filter.tiltakskoder,
                        oppgavetyper = filter.oppgavetyper,
                        kostnadssteder = kostnadssteder,
                        roller = ansattesRoller,
                    ),
                )
            }
        }
    }

    fun tilsagnOppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        roller: Set<Rolle>,
        ansatt: NavIdent,
    ): List<Oppgave> = db.session {
        queries.tilsagn
            .getAll(
                statuser = listOf(
                    TilsagnStatus.TIL_GODKJENNING,
                    TilsagnStatus.TIL_ANNULLERING,
                    TilsagnStatus.TIL_OPPGJOR,
                    TilsagnStatus.RETURNERT,
                ),
            )
            .asSequence()
            .filter { oppgave ->
                kostnadssteder.isEmpty() || oppgave.kostnadssted.enhetsnummer in kostnadssteder
            }
            .filter { tiltakskoder.isEmpty() || it.tiltakstype.tiltakskode in tiltakskoder }
            .mapNotNull { toOppgave(it) }
            .mapNotNull { (totrinnskontroll, oppgave) ->
                oppgave.takeIf { totrinnskontroll.behandletAv != ansatt }
            }
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
            .filter { it.type.rolle in roller }
            .toList()
    }

    fun delutbetalingOppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavIdent,
        roller: Set<Rolle>,
    ): List<Oppgave> = db.session {
        queries.delutbetaling
            .getOppgaveData(
                kostnadssteder = kostnadssteder.ifEmpty { null },
                tiltakskoder = tiltakskoder.ifEmpty { null },
            )
            .mapNotNull { toOppgave(it) }
            .mapNotNull { (totrinnskontroll, oppgave) ->
                oppgave.takeIf { totrinnskontroll.behandletAv != ansatt }
            }
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
            .filter { it.type.rolle in roller }
    }

    fun utbetalingOppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        roller: Set<Rolle>,
    ): List<Oppgave> = db.session {
        queries.utbetaling
            .getOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> utbetaling.innsender == Arrangor }
            .filter { utbetaling -> queries.delutbetaling.getByUtbetalingId(utbetaling.id).isEmpty() }
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .map { toOppgave(it) }
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
            .filter { it.type.rolle in roller }
            .toList()
    }

    private fun QueryContext.byKostnadssted(
        utbetaling: Utbetaling,
        kostnadssteder: Set<NavEnhetNummer>,
    ): Boolean = when {
        kostnadssteder.isEmpty() -> true
        else -> {
            queries.tilsagn
                .getAll(gjennomforingId = utbetaling.gjennomforing.id, periodeIntersectsWith = utbetaling.periode)
                .let { tilsagn -> tilsagn.isEmpty() || tilsagn.any { it.kostnadssted.enhetsnummer in kostnadssteder } }
        }
    }

    private fun getNavEnheterForRegioner(regioner: Set<NavEnhetNummer>): Set<NavEnhetNummer> = db.session {
        regioner.flatMapTo(mutableSetOf()) { region ->
            queries.enhet.getAll(overordnetEnhet = region).map { it.enhetsnummer }
        }
    }
}

private fun QueryContext.toOppgave(tilsagn: Tilsagn): Pair<Totrinnskontroll, Oppgave>? {
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
            opprettelse to Oppgave(
                id = tilsagn.id,
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
            opprettelse to Oppgave(
                id = tilsagn.id,
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
            annullering to Oppgave(
                id = tilsagn.id,
                type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                title = "Tilsagn til annullering",
                description = "Tilsagnet for ${tilsagn.gjennomforing.navn} er sendt til annullering",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = annullering.behandletTidspunkt,
                oppgaveIcon = OppgaveIcon.TILSAGN,
            )
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            val tilOppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
            tilOppgjor to Oppgave(
                id = tilsagn.id,
                type = OppgaveType.TILSAGN_TIL_OPPGJOR,
                title = "Tilsagn til oppgjør",
                description = "Tilsagnet for ${tilsagn.gjennomforing.navn} er sendt til oppgjør",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = tilOppgjor.behandletTidspunkt,
                oppgaveIcon = OppgaveIcon.TILSAGN,
            )
        }

        TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT -> null
    }
}

private fun QueryContext.toOppgave(oppgavedata: DelutbetalingOppgaveData): Pair<Totrinnskontroll, Oppgave>? {
    val (delutbetaling, gjennomforingId, gjennomforingsnavn, tiltakstype) = oppgavedata
    val link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/$gjennomforingId/utbetalinger/${delutbetaling.utbetalingId}",
    )
    return when (delutbetaling.status) {
        DelutbetalingStatus.TIL_GODKJENNING -> {
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            opprettelse to Oppgave(
                id = delutbetaling.id,
                type = OppgaveType.UTBETALING_TIL_GODKJENNING,
                title = "Utbetaling til godkjenning",
                description = "Utbetalingen for $gjennomforingsnavn er sendt til godkjenning",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.behandletTidspunkt,
                oppgaveIcon = OppgaveIcon.UTBETALING,
            )
        }

        DelutbetalingStatus.RETURNERT -> {
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            opprettelse to Oppgave(
                id = delutbetaling.id,
                type = OppgaveType.UTBETALING_RETURNERT,
                title = "Utbetaling returnert",
                description = "Utbetaling for $gjennomforingsnavn ble returnert av beslutter",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = requireNotNull(opprettelse.besluttetTidspunkt),
                oppgaveIcon = OppgaveIcon.UTBETALING,
            )
        }

        else -> null
    }
}

private fun toOppgave(utbetaling: Utbetaling): Oppgave = Oppgave(
    id = utbetaling.id,
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
