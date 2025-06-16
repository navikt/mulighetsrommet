package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingOppgaveData
import no.nav.mulighetsrommet.api.navansatt.helper.NavAnsattRolleHelper
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingOppgaveData
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.*

class OppgaverService(val db: ApiDatabase) {
    fun oppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        ansatt: NavIdent,
        roller: Set<NavAnsattRolle>,
    ): List<Oppgave> {
        val navEnheterForRegioner = getNavEnheterForRegioner(regioner)

        val oppgaver = buildList {
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it in OppgaveType.TilsagnOppgaver }) {
                addAll(
                    tilsagnOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it in OppgaveType.DelutbetalingOppgaver }) {
                addAll(
                    delutbetalingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it in OppgaveType.UtbetalingOppgaver }) {
                addAll(
                    utbetalingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it in OppgaveType.AvtaleOppgaver }) {
                addAll(
                    avtaleOppgaver(
                        tiltakskoder = tiltakskoder,
                        regioner = regioner,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it in OppgaveType.GjennomforingOppgaver }) {
                addAll(
                    gjennomforingOppgaver(
                        tiltakskoder = tiltakskoder,
                        navEnheter = navEnheterForRegioner,
                    ),
                )
            }
        }

        return oppgaver
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
            .filter { oppgave ->
                val requiredRole = NavAnsattRolle
                    .kontorspesifikk(oppgave.type.rolle, setOfNotNull(oppgave.enhet?.nummer))
                NavAnsattRolleHelper.hasRole(roller, requiredRole)
            }
    }

    private fun tilsagnOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
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
                oppgave.takeIf { totrinnskontroll.behandletAv != ansatt || oppgave.type == OppgaveType.TILSAGN_RETURNERT }
            }
            .toList()
    }

    private fun delutbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavIdent,
    ): List<Oppgave> = db.session {
        queries.delutbetaling
            .getOppgaveData(
                kostnadssteder = kostnadssteder.ifEmpty { null },
                tiltakskoder = tiltakskoder.ifEmpty { null },
            )
            .asSequence()
            .mapNotNull { toOppgave(it) }
            .mapNotNull { (totrinnskontroll, oppgave) ->
                oppgave.takeIf { totrinnskontroll.behandletAv != ansatt || oppgave.type == OppgaveType.UTBETALING_RETURNERT }
            }
            .toList()
    }

    private fun utbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        queries.utbetaling
            .getOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> utbetaling.innsender == Arrangor }
            .filter { utbetaling -> queries.delutbetaling.getByUtbetalingId(utbetaling.id).isEmpty() }
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .map { toOppgave(it) }
            .toList()
    }

    private fun avtaleOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        val tiltakstypeIds = queries.tiltakstype.getAll().filter { it.tiltakskode in tiltakskoder }.map { it.id }

        queries.avtale
            .getAll(
                tiltakstypeIder = tiltakstypeIds,
                navRegioner = regioner.toList(),
                statuser = listOf(AvtaleStatus.UTKAST, AvtaleStatus.AKTIV),
            )
            .items
            .flatMap { toOppgaver(it) }
    }

    private fun gjennomforingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        queries.gjennomforing
            .getOppgaveData(tiltakskoder = tiltakskoder)
            .filter { navEnheter.isEmpty() || it.kontorstruktur.flatMap { it.kontorer }.any { it.enhetsnummer in navEnheter } }
            .flatMap { toOppgaver(it) }
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
            queries.enhet.getAll(overordnetEnhet = region).map { it.enhetsnummer } + region
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
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
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
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
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
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
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
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
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
        DelutbetalingStatus.TIL_ATTESTERING -> {
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            opprettelse to Oppgave(
                id = delutbetaling.id,
                type = OppgaveType.UTBETALING_TIL_ATTESTERING,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = "Utbetaling til attestering",
                description = "Utbetalingen for $gjennomforingsnavn er sendt til attestering",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.behandletTidspunkt,
                oppgaveIcon = OppgaveIcon.UTBETALING,
            )
        }

        DelutbetalingStatus.RETURNERT -> {
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            opprettelse to Oppgave(
                id = delutbetaling.id,
                type = OppgaveType.UTBETALING_RETURNERT,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = "Utbetaling returnert",
                description = "Utbetaling for $gjennomforingsnavn ble returnert av attestant",
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
    enhet = null,
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

private fun QueryContext.toOppgaver(avtale: AvtaleDto): List<Oppgave> = buildList {
    if (avtale.administratorer.isEmpty()) {
        val updatedAt = queries.avtale.getUpdatedAt(avtale.id)
        add(
            Oppgave(
                id = avtale.id,
                type = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR,
                title = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR.navn,
                enhet = avtale.kontorstruktur.firstOrNull()?.region?.let {
                    OppgaveEnhet(
                        nummer = it.enhetsnummer,
                        navn = it.navn,
                    )
                },
                description = """Avtalen "${avtale.navn}" mangler administrator. Gå til avtalen og sett deg som administrator hvis du eier avtalen.""",
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = avtale.tiltakstype.tiltakskode,
                    navn = avtale.tiltakstype.navn,
                ),
                link = OppgaveLink(
                    linkText = "Se avtale",
                    link = "/avtaler/${avtale.id}",
                ),
                createdAt = updatedAt,
                oppgaveIcon = OppgaveIcon.AVTALE,
            ),
        )
    }
}

private fun toOppgaver(data: GjennomforingOppgaveData): List<Oppgave> = buildList {
    add(
        Oppgave(
            id = data.id,
            type = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR,
            title = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR.navn,
            enhet = data.kontorstruktur.firstOrNull()?.region?.let {
                OppgaveEnhet(
                    nummer = it.enhetsnummer,
                    navn = it.navn,
                )
            },
            description = """Gjennomføringen "${data.navn}" mangler administrator. Gå til gjennomføringen og sett deg som administrator hvis du eier gjennomføringen.""",
            tiltakstype = OppgaveTiltakstype(
                tiltakskode = data.tiltakskode,
                navn = data.tiltakstypeNavn,
            ),
            link = OppgaveLink(
                linkText = "Se gjennomføring",
                link = "/gjennomforinger/${data.id}",
            ),
            createdAt = data.updatedAt,
            oppgaveIcon = OppgaveIcon.AVTALE,
        ),
    )
}
