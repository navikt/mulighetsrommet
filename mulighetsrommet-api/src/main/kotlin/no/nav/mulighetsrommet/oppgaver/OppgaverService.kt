package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.navansatt.helper.NavAnsattRolleHelper
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.tilsagnHandlinger
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.linjeHandlinger
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.utbetalingHandlinger
import no.nav.mulighetsrommet.model.*

class OppgaverService(val db: ApiDatabase) {
    fun oppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
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
                        ansatt = ansatt,
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
                NavAnsattRolleHelper.hasRole(ansatt.roller, requiredRole)
            }
    }

    private fun tilsagnOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> = db.session {
        queries.oppgave
            .getTilsagnOppgaveData()
            .asSequence()
            .filter { oppgave ->
                kostnadssteder.isEmpty() || oppgave.kostnadssted in kostnadssteder
            }
            .filter { tiltakskoder.isEmpty() || it.tiltakstype.tiltakskode in tiltakskoder }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun delutbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        ansatt: NavAnsatt,
    ): List<Oppgave> = db.session {
        queries.oppgave
            .getDelutbetalingOppgaveData(
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
        ansatt: NavAnsatt,
    ): List<Oppgave> = db.session {
        queries.oppgave
            .getUtbetalingOppgaveData(tiltakskoder = tiltakskoder.ifEmpty { null })
            .asSequence()
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun avtaleOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        queries.oppgave
            .getAvtaleOppgaveData(
                tiltakskoder = tiltakskoder,
                navRegioner = regioner.toList(),
            )
            .map { it.toOppgave() }
    }

    private fun gjennomforingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
    ): List<Oppgave> = db.session {
        queries.oppgave
            .getGjennomforingOppgaveData(tiltakskoder = tiltakskoder)
            .filter { navEnheter.isEmpty() || it.kontorstruktur.flatMap { it.kontorer }.any { it.enhetsnummer in navEnheter } }
            .map { it.toOppgave() }
    }

    private fun QueryContext.byKostnadssted(
        data: UtbetalingOppgaveData,
        kostnadssteder: Set<NavEnhetNummer>,
    ): Boolean = when {
        kostnadssteder.isEmpty() -> true
        else -> {
            queries.oppgave
                .getUtbetalingKostnadssteder(data.gjennomforingId, data.periode)
                .let { it.isEmpty() || it.any { it in kostnadssteder } }
        }
    }

    private fun getNavEnheterForRegioner(regioner: Set<NavEnhetNummer>): Set<NavEnhetNummer> = db.session {
        regioner.flatMapTo(mutableSetOf()) { region ->
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
        link = "/gjennomforinger/${data.gjennomforingId}/tilsagn/${data.id}",
    )

    val opprettelse = queries.totrinnskontroll.getOrError(data.id, Totrinnskontroll.Type.OPPRETT)
    val annullering = queries.totrinnskontroll.get(data.id, Totrinnskontroll.Type.ANNULLER)
    val tilOppgjor = queries.totrinnskontroll.get(data.id, Totrinnskontroll.Type.GJOR_OPP)

    val handlinger = tilsagnHandlinger(
        id = data.id,
        kostnadssted = data.kostnadssted,
        status = data.status,
        belopBrukt = data.belopBrukt,
        ansatt = ansatt,
    )

    return when (data.status) {
        TilsagnStatus.TIL_GODKJENNING -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_GODKJENNING,
                navn = OppgaveType.TILSAGN_TIL_GODKJENNING.navn,
                enhet = OppgaveEnhet(navn = data.kostnadsstedNavn, nummer = data.kostnadssted),
                title = data.gjennomforingNavn,
                description = "Tilsagnet ${data.bestillingsnummer} er sendt til godkjenning",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.behandletTidspunkt,
                iconType = OppgaveIconType.TILSAGN,
            ).takeIf {
                handlinger.contains(TilsagnHandling.GODKJENN)
            }
        }

        TilsagnStatus.RETURNERT -> {
            requireNotNull(opprettelse.besluttetTidspunkt)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_RETURNERT,
                navn = OppgaveType.TILSAGN_RETURNERT.navn,
                title = data.gjennomforingNavn,
                enhet = OppgaveEnhet(navn = data.kostnadsstedNavn, nummer = data.kostnadssted),
                description = "Tilsagnet ${data.bestillingsnummer} er returnert av beslutter",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = opprettelse.besluttetTidspunkt,
                iconType = OppgaveIconType.TILSAGN,
            ).takeIf {
                handlinger.contains(TilsagnHandling.REDIGER)
            }
        }

        TilsagnStatus.TIL_ANNULLERING -> {
            requireNotNull(annullering)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_ANNULLERING,
                navn = OppgaveType.TILSAGN_TIL_ANNULLERING.navn,
                enhet = OppgaveEnhet(navn = data.kostnadsstedNavn, nummer = data.kostnadssted),
                title = data.gjennomforingNavn,
                description = "Tilsagnet ${data.bestillingsnummer} er sendt til annullering",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = annullering.behandletTidspunkt,
                iconType = OppgaveIconType.TILSAGN,
            ).takeIf {
                handlinger.contains(TilsagnHandling.GODKJENN_ANNULLERING)
            }
        }

        TilsagnStatus.TIL_OPPGJOR -> {
            requireNotNull(tilOppgjor)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_TIL_OPPGJOR,
                navn = OppgaveType.TILSAGN_TIL_OPPGJOR.navn,
                enhet = OppgaveEnhet(navn = data.kostnadsstedNavn, nummer = data.kostnadssted),
                title = data.gjennomforingNavn,
                description = "Tilsagnet ${data.bestillingsnummer} er klar til oppgjør",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = tilOppgjor.behandletTidspunkt,
                iconType = OppgaveIconType.TILSAGN,
            ).takeIf {
                handlinger.contains(TilsagnHandling.GODKJENN_OPPGJOR)
            }
        }

        TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT -> null
    }
}

private fun QueryContext.toOppgave(data: DelutbetalingOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/${data.gjennomforingId}/utbetalinger/${data.utbetalingId}",
    )
    val opprettelse = queries.totrinnskontroll.getOrError(data.id, Totrinnskontroll.Type.OPPRETT)

    val tilsagn = queries.tilsagn.getOrError(data.tilsagnId)
    val handlinger = linjeHandlinger(opprettelse, tilsagn.kostnadssted.enhetsnummer, ansatt)

    return when (data.status) {
        DelutbetalingStatus.TIL_ATTESTERING -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_TIL_ATTESTERING,
                navn = OppgaveType.UTBETALING_TIL_ATTESTERING.navn,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = data.gjennomforingNavn,
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er klar til attestering",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = opprettelse.behandletTidspunkt,
                iconType = OppgaveIconType.UTBETALING,
            ).takeIf {
                handlinger.contains(UtbetalingLinjeHandling.ATTESTER)
            }
        }
        DelutbetalingStatus.RETURNERT -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_RETURNERT,
                navn = OppgaveType.UTBETALING_RETURNERT.navn,
                enhet = tilsagn.kostnadssted.let {
                    OppgaveEnhet(navn = it.navn, nummer = it.enhetsnummer)
                },
                title = data.gjennomforingNavn,
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er returnert av attestant",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = requireNotNull(opprettelse.besluttetTidspunkt),
                iconType = OppgaveIconType.UTBETALING,
            ).takeIf {
                handlinger.contains(UtbetalingLinjeHandling.SEND_TIL_ATTESTERING)
            }
        }

        else -> null
    }
}

private fun toOppgave(data: UtbetalingOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val handlinger = utbetalingHandlinger(data.status, ansatt)
    return when (data.status) {
        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.RETURNERT,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        -> null
        UtbetalingStatusType.INNSENDT ->
            Oppgave(
                id = data.id,
                type = OppgaveType.UTBETALING_TIL_BEHANDLING,
                navn = OppgaveType.UTBETALING_TIL_BEHANDLING.navn,
                enhet = null,
                title = data.gjennomforingNavn,
                description = "Utbetaling for perioden ${data.periode.formatPeriode()} er klar til behandling",
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = data.tiltakstype.tiltakskode,
                    navn = data.tiltakstype.navn,
                ),
                link = OppgaveLink(
                    linkText = "Se utbetaling",
                    link = "/gjennomforinger/${data.gjennomforingId}/utbetalinger/${data.id}",
                ),
                createdAt = data.createdAt,
                iconType = OppgaveIconType.UTBETALING,
            ).takeIf { handlinger.contains(UtbetalingHandling.SEND_TIL_ATTESTERING) }
    }
}

private fun AvtaleOppgaveData.toOppgave() = Oppgave(
    id = this.id,
    type = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR,
    navn = OppgaveType.AVTALE_MANGLER_ADMINISTRATOR.navn,
    title = this.navn,
    enhet = this.kontorstruktur.firstOrNull()?.region?.let {
        OppgaveEnhet(
            nummer = it.enhetsnummer,
            navn = it.navn,
        )
    },
    description = """Gå til avtalen og sett deg som administrator hvis du eier avtalen.""",
    tiltakstype = OppgaveTiltakstype(
        tiltakskode = this.tiltakstype.tiltakskode,
        navn = this.tiltakstype.navn,
    ),
    link = OppgaveLink(
        linkText = "Se avtale",
        link = "/avtaler/${this.id}",
    ),
    createdAt = this.createdAt,
    iconType = OppgaveIconType.AVTALE,
)

private fun GjennomforingOppgaveData.toOppgave() = Oppgave(
    id = this.id,
    type = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR,
    navn = OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR.navn,
    title = this.navn,
    enhet = this.kontorstruktur.firstOrNull()?.region?.let {
        OppgaveEnhet(
            nummer = it.enhetsnummer,
            navn = it.navn,
        )
    },
    description = """Gå til gjennomføringen og sett deg som administrator hvis du eier gjennomføringen.""",
    tiltakstype = OppgaveTiltakstype(
        tiltakskode = this.tiltakskode,
        navn = this.tiltakstypeNavn,
    ),
    link = OppgaveLink(
        linkText = "Se gjennomføring",
        link = "/gjennomforinger/${this.id}",
    ),
    createdAt = this.updatedAt,
    iconType = OppgaveIconType.GJENNOMFORING,
)
