package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode

class OppgaverService(val db: ApiDatabase) {
    fun oppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> = db.transaction {
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
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.AVTALE }) {
                addAll(
                    avtaleOppgaver(
                        tiltakskoder = tiltakskoder,
                        regioner = regioner,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.GJENNOMFORING }) {
                addAll(
                    gjennomforingOppgaver(
                        tiltakskoder = tiltakskoder,
                        navEnheter = navEnheterForRegioner,
                        ansatt = ansatt,
                    ),
                )
            }
        }

        return oppgaver
            .filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
    }

    private fun QueryContext.tilsagnOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getTilsagnOppgaveData()
            .asSequence()
            .filter { oppgave ->
                kostnadssteder.isEmpty() || oppgave.kostnadssted.nummer in kostnadssteder
            }
            .filter { tiltakskoder.isEmpty() || it.tiltakstype.tiltakskode in tiltakskoder }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.delutbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getDelutbetalingOppgaveData(
                kostnadssteder = kostnadssteder.ifEmpty { null },
                tiltakskoder = tiltakskoder.ifEmpty { null },
            )
            .asSequence()
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.utbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getUtbetalingOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.avtaleOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getAvtaleManglerAdministratorOppgaveData(tiltakskoder, regioner)
            .mapNotNull { it.toOppgave(ansatt) }
    }

    private fun QueryContext.gjennomforingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getGjennomforingManglerAdministratorOppgaveData(tiltakskoder, navEnheter)
            .mapNotNull { it.toOppgave(ansatt) }
    }

    private fun byKostnadssted(
        data: UtbetalingOppgaveData,
        kostnadssteder: Set<NavEnhetNummer>,
    ): Boolean = when {
        kostnadssteder.isEmpty() -> true

        else -> {
            data.kostnadssteder.isEmpty() || data.kostnadssteder.any { it in kostnadssteder }
        }
    }

    private fun QueryContext.getNavEnheterForRegioner(regioner: Set<NavEnhetNummer>): Set<NavEnhetNummer> {
        return regioner.flatMapTo(mutableSetOf()) { region ->
            queries.enhet.getAll(overordnetEnhet = region).map { it.enhetsnummer } + region
        }
    }
}

private fun QueryContext.toOppgave(data: TilsagnOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val tiltakstype = OppgaveTiltakstype(
        tiltakskode = data.tiltakstype.tiltakskode,
        navn = data.tiltakstype.navn,
    )

    val link = OppgaveLink(
        linkText = "Se tilsagn",
        link = "/gjennomforinger/${data.gjennomforing.id}/tilsagn/${data.id}",
    )

    val opprettelse = queries.totrinnskontroll.getOrError(data.id, Totrinnskontroll.Type.OPPRETT)
    val annullering = queries.totrinnskontroll.get(data.id, Totrinnskontroll.Type.ANNULLER)
    val tilOppgjor = queries.totrinnskontroll.get(data.id, Totrinnskontroll.Type.GJOR_OPP)

    var title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing)
    return when (data.status) {
        TilsagnStatus.TIL_GODKJENNING -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_GODKJENNING,
                navn = OppgaveType.TILSAGN_TIL_GODKJENNING.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilsagnet ${data.bestillingsnummer} er sendt til godkjenning",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.behandletTidspunkt,
            ).takeIf {
                TilsagnService.tilgangTilHandling(
                    TilsagnHandling.GODKJENN,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                )
            }
        }

        TilsagnStatus.RETURNERT -> {
            requireNotNull(opprettelse.besluttetTidspunkt)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_RETURNERT,
                navn = OppgaveType.TILSAGN_RETURNERT.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilsagnet ${data.bestillingsnummer} er returnert av beslutter",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.besluttetTidspunkt,
            ).takeIf {
                TilsagnService.tilgangTilHandling(
                    TilsagnHandling.REDIGER,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                )
            }
        }

        TilsagnStatus.TIL_ANNULLERING -> {
            requireNotNull(annullering)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                navn = OppgaveType.TILSAGN_TIL_ANNULLERING.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilsagnet ${data.bestillingsnummer} er sendt til annullering",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = annullering.behandletTidspunkt,
            ).takeIf {
                TilsagnService.tilgangTilHandling(
                    TilsagnHandling.GODKJENN_ANNULLERING,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                )
            }
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            requireNotNull(tilOppgjor)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_OPPGJOR,
                navn = OppgaveType.TILSAGN_TIL_OPPGJOR.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilsagnet ${data.bestillingsnummer} er klar til oppgjør",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = tilOppgjor.behandletTidspunkt,
            ).takeIf {
                TilsagnService.tilgangTilHandling(
                    TilsagnHandling.GODKJENN_OPPGJOR,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = tilOppgjor,
                )
            }
        }

        TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT -> null
    }
}

private fun toOppgave(data: DelutbetalingOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/${data.gjennomforing.id}/utbetalinger/${data.utbetalingId}",
    )

    return when (data.status) {
        DelutbetalingStatus.TIL_ATTESTERING -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_TIL_ATTESTERING,
                navn = OppgaveType.UTBETALING_TIL_ATTESTERING.navn,
                enhet = data.kostnadssted,
                title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing),
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er klar til attestering",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = data.opprettelse.behandletTidspunkt,
            ).takeIf {
                UtbetalingService.tilgangTilHandling(
                    handling = UtbetalingLinjeHandling.ATTESTER,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    behandletAv = data.opprettelse.behandletAv,
                )
            }
        }

        DelutbetalingStatus.RETURNERT -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_RETURNERT,
                navn = OppgaveType.UTBETALING_RETURNERT.navn,
                enhet = data.kostnadssted,
                title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing),
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er returnert av attestant",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = requireNotNull(data.opprettelse.besluttetTidspunkt),
            ).takeIf {
                UtbetalingService.tilgangTilHandling(
                    handling = UtbetalingLinjeHandling.SEND_TIL_ATTESTERING,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    behandletAv = data.opprettelse.behandletAv,
                )
            }
        }

        else -> null
    }
}

private fun toOppgave(data: UtbetalingOppgaveData, ansatt: NavAnsatt): Oppgave? {
    return when (data.status) {
        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.RETURNERT,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.UTBETALT,
        -> null

        UtbetalingStatusType.INNSENDT ->
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_TIL_BEHANDLING,
                navn = OppgaveType.UTBETALING_TIL_BEHANDLING.navn,
                enhet = null,
                title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing),
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er klar til behandling",
                tiltakstype = data.tiltakstype,
                link = OppgaveLink(
                    linkText = "Se utbetaling",
                    link = "/gjennomforinger/${data.gjennomforing.id}/utbetalinger/${data.id}",
                ),
                createdAt = data.godkjentAvArrangorTidspunkt ?: data.createdAt,
            ).takeIf { UtbetalingService.tilgangTilHandling(UtbetalingHandling.SEND_TIL_ATTESTERING, ansatt) }
    }
}

private fun AvtaleManglerAdministratorOppgaveData.toOppgave(ansatt: NavAnsatt) = Oppgave(
    id = id,
    type = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR,
    navn = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR.navn,
    enhet = kontorstruktur.firstOrNull()?.region?.let {
        OppgaveEnhet(
            nummer = it.enhetsnummer,
            navn = it.navn,
        )
    },
    title = navn,
    description = """Gå til avtalen og sett deg som administrator hvis du eier avtalen.""",
    tiltakstype = tiltakstype,
    link = OppgaveLink(
        linkText = "Se avtale",
        link = "/avtaler/$id",
    ),
    createdAt = oppdatertTidspunkt,
).takeIf {
    AvtaleService.tilgangTilHandling(AvtaleHandling.REDIGER, ansatt)
}

private fun GjennomforingManglerAdministratorOppgaveData.toOppgave(ansatt: NavAnsatt) = Oppgave(
    id = id,
    type = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR,
    navn = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR.navn,
    enhet = kontorstruktur.firstOrNull()?.region?.let {
        OppgaveEnhet(
            nummer = it.enhetsnummer,
            navn = it.navn,
        )
    },
    title = navn,
    description = """Gå til gjennomføringen og sett deg som administrator hvis du eier gjennomføringen.""",
    tiltakstype = tiltakstype,
    link = OppgaveLink(
        linkText = "Se gjennomføring",
        link = "/gjennomforinger/$id",
    ),
    createdAt = oppdatertTidspunkt,
).takeIf {
    GjennomforingService.tilgangTilHandling(GjennomforingHandling.REDIGER, ansatt)
}

private fun getOkonomiOppgaveTitle(tiltakstype: OppgaveTiltakstype, gjennomforing: OppgaveGjennomforing): String {
    return "${tiltakstype.navn} (${gjennomforing.lopenummer.value})"
}
