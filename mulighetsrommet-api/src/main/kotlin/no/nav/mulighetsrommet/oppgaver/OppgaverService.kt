package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingOppgaveData
import no.nav.mulighetsrommet.api.navansatt.helper.NavAnsattRolleHelper
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingOppgaveData
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
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
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.TILSAGN }) {
                addAll(
                    tilsagnOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.DELUTBETALING }) {
                addAll(
                    delutbetalingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.UTBETALING }) {
                addAll(
                    utbetalingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.AVTALE }) {
                addAll(
                    avtaleOppgaver(
                        tiltakskoder = tiltakskoder,
                        regioner = regioner,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.GJENNOMFORING }) {
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
            .mapNotNull { toOppgave(it, ansatt) }
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
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun utbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        queries.utbetaling
            .getOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .mapNotNull { toOppgave(it) }
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
                statuser = listOf(AvtaleStatusType.UTKAST, AvtaleStatusType.AKTIV),
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

private fun QueryContext.toOppgave(tilsagn: Tilsagn, ansatt: NavIdent): Oppgave? {
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
            if (opprettelse.behandletAv == ansatt) {
                null
            } else {
                Oppgave(
                    id = tilsagn.id,
                    type = OppgaveType.TILSAGN_TIL_GODKJENNING,
                    navn = OppgaveType.TILSAGN_TIL_GODKJENNING.navn,
                    enhet = tilsagn.kostnadssted.let {
                        OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                    },
                    title = tilsagn.gjennomforing.navn,
                    description = "Tilsagnet ${tilsagn.bestilling.bestillingsnummer} er sendt til godkjenning",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = opprettelse.behandletTidspunkt,
                    iconType = OppgaveIconType.TILSAGN,
                )
            }
        }

        TilsagnStatus.RETURNERT -> {
            val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
            requireNotNull(opprettelse.besluttetTidspunkt)
            Oppgave(
                id = tilsagn.id,
                type = OppgaveType.TILSAGN_RETURNERT,
                navn = OppgaveType.TILSAGN_RETURNERT.navn,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = tilsagn.gjennomforing.navn,
                description = "Tilsagnet ${tilsagn.bestilling.bestillingsnummer} er returnert av beslutter",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.besluttetTidspunkt,
                iconType = OppgaveIconType.TILSAGN,
            )
        }

        TilsagnStatus.TIL_ANNULLERING -> {
            val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
            if (annullering.behandletAv == ansatt) {
                null
            } else {
                Oppgave(
                    id = tilsagn.id,
                    type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                    navn = OppgaveType.TILSAGN_TIL_ANNULLERING.navn,
                    enhet = tilsagn.kostnadssted.let {
                        OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                    },
                    title = tilsagn.gjennomforing.navn,
                    description = "Tilsagnet ${tilsagn.bestilling.bestillingsnummer} er sendt til annullering",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = annullering.behandletTidspunkt,
                    iconType = OppgaveIconType.TILSAGN,
                )
            }
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            val tilOppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
            if (tilOppgjor.behandletAv == ansatt) {
                null
            } else {
                Oppgave(
                    id = tilsagn.id,
                    type = OppgaveType.TILSAGN_TIL_OPPGJOR,
                    navn = OppgaveType.TILSAGN_TIL_OPPGJOR.navn,
                    enhet = tilsagn.kostnadssted.let {
                        OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                    },
                    title = tilsagn.gjennomforing.navn,
                    description = "Tilsagnet ${tilsagn.bestilling.bestillingsnummer} er klar til oppgjør",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = tilOppgjor.behandletTidspunkt,
                    iconType = OppgaveIconType.TILSAGN,
                )
            }
        }

        TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT -> null
    }
}

private fun QueryContext.toOppgave(oppgavedata: DelutbetalingOppgaveData, ansatt: NavIdent): Oppgave? {
    val (delutbetaling, gjennomforingId, gjennomforingsnavn, tiltakstype) = oppgavedata
    val link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/$gjennomforingId/utbetalinger/${delutbetaling.utbetalingId}",
    )
    return when (delutbetaling.status) {
        DelutbetalingStatus.TIL_ATTESTERING -> {
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            val tilsagnOpprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)

            if (opprettelse.behandletAv == ansatt || tilsagnOpprettelse.besluttetAv == ansatt) {
                null
            } else {
                Oppgave(
                    id = delutbetaling.id,
                    type = OppgaveType.UTBETALING_TIL_ATTESTERING,
                    navn = OppgaveType.UTBETALING_TIL_ATTESTERING.navn,
                    enhet = tilsagn.kostnadssted.let {
                        OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                    },
                    title = gjennomforingsnavn,
                    description = "Utbetaling for perioden ${delutbetaling.periode.formatPeriode()} er klar til attestering",
                    tiltakstype = tiltakstype,
                    link = link,
                    createdAt = opprettelse.behandletTidspunkt,
                    iconType = OppgaveIconType.UTBETALING,
                )
            }
        }
        DelutbetalingStatus.RETURNERT -> {
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            Oppgave(
                id = delutbetaling.id,
                type = OppgaveType.UTBETALING_RETURNERT,
                navn = OppgaveType.UTBETALING_RETURNERT.navn,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = gjennomforingsnavn,
                description = "Utbetaling for perioden ${delutbetaling.periode.formatPeriode()} er returnert av attestant",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = requireNotNull(opprettelse.besluttetTidspunkt),
                iconType = OppgaveIconType.UTBETALING,
            )
        }

        else -> null
    }
}

private fun toOppgave(utbetaling: Utbetaling): Oppgave? = when (utbetaling.status) {
    UtbetalingStatusType.GENERERT,
    UtbetalingStatusType.TIL_ATTESTERING,
    UtbetalingStatusType.RETURNERT,
    UtbetalingStatusType.FERDIG_BEHANDLET,
    -> null
    UtbetalingStatusType.INNSENDT ->
        Oppgave(
            id = utbetaling.id,
            type = OppgaveType.UTBETALING_TIL_BEHANDLING,
            navn = OppgaveType.UTBETALING_TIL_BEHANDLING.navn,
            enhet = null,
            title = utbetaling.gjennomforing.navn,
            description = "Utbetaling for perioden ${utbetaling.periode.formatPeriode()} er klar til behandling",
            tiltakstype = OppgaveTiltakstype(
                tiltakskode = utbetaling.tiltakstype.tiltakskode,
                navn = utbetaling.tiltakstype.navn,
            ),
            link = OppgaveLink(
                linkText = "Se utbetaling",
                link = "/gjennomforinger/${utbetaling.gjennomforing.id}/utbetalinger/${utbetaling.id}",
            ),
            createdAt = utbetaling.createdAt,
            iconType = OppgaveIconType.UTBETALING,
        ).takeIf { utbetaling.innsender == Arrangor }
}

private fun QueryContext.toOppgaver(avtale: Avtale): List<Oppgave> = buildList {
    if (avtale.administratorer.isEmpty()) {
        val updatedAt = queries.avtale.getUpdatedAt(avtale.id)
        add(
            Oppgave(
                id = avtale.id,
                type = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR,
                navn = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR.navn,
                title = avtale.navn,
                enhet = avtale.kontorstruktur.firstOrNull()?.region?.let {
                    OppgaveEnhet(
                        nummer = it.enhetsnummer,
                        navn = it.navn,
                    )
                },
                description = """Gå til avtalen og sett deg som administrator hvis du eier avtalen.""",
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = avtale.tiltakstype.tiltakskode,
                    navn = avtale.tiltakstype.navn,
                ),
                link = OppgaveLink(
                    linkText = "Se avtale",
                    link = "/avtaler/${avtale.id}",
                ),
                createdAt = updatedAt,
                iconType = OppgaveIconType.AVTALE,
            ),
        )
    }
}

private fun toOppgaver(data: GjennomforingOppgaveData): List<Oppgave> = buildList {
    add(
        Oppgave(
            id = data.id,
            type = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR,
            navn = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR.navn,
            title = data.navn,
            enhet = data.kontorstruktur.firstOrNull()?.region?.let {
                OppgaveEnhet(
                    nummer = it.enhetsnummer,
                    navn = it.navn,
                )
            },
            description = """Gå til gjennomføringen og sett deg som administrator hvis du eier gjennomføringen.""",
            tiltakstype = OppgaveTiltakstype(
                tiltakskode = data.tiltakskode,
                navn = data.tiltakstypeNavn,
            ),
            link = OppgaveLink(
                linkText = "Se gjennomføring",
                link = "/gjennomforinger/${data.id}",
            ),
            createdAt = data.updatedAt,
            iconType = OppgaveIconType.GJENNOMFORING,
        ),
    )
}
