package no.nav.mulighetsrommet.oppgaver

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingDetaljerService
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.AdminUtbetalingService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class OppgaverService(val db: ApiDatabase, private val features: FeatureToggleService) {
    fun getOppgavetyper(ansatt: NavAnsatt): List<OppgaveTypeDto> {
        val roller = ansatt.roller.map { it.rolle }.toSet()

        return OppgaveType.entries
            .filter { it.rolle in roller }
            .filter {
                isEnkeltplassEnabled() || it.kategori !in setOf(
                    Kategori.ENKELTPLASS,
                    Kategori.TILSKUDDBEHANDLING,
                )
            }
            .map { OppgaveTypeDto(navn = it.navn, type = it) }
    }

    fun oppgaver(
        oppgavetyper: Set<OppgaveType>,
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> = db.transaction {
        val navEnheterForRegioner = if (regioner.isEmpty()) {
            emptySet()
        } else {
            getNavEnheterOgKostnadsstederForRegioner(regioner)
        }

        val oppgaver = buildList {
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.TILSAGN }) {
                addAll(
                    tilsagnOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.UTBETALING_LINJE }) {
                addAll(
                    utbetalingLinjeOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.UTBETALING }) {
                addAll(
                    utbetalingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.AVTALE }) {
                addAll(
                    avtaleOppgaver(
                        tiltakskoder = tiltakskoder,
                        regioner = regioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.GJENNOMFORING }) {
                addAll(
                    gjennomforingOppgaver(
                        tiltakskoder = tiltakskoder,
                        navEnheter = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (isEnkeltplassEnabled() && (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.ENKELTPLASS })) {
                addAll(
                    enkeltplassOppgaver(
                        tiltakskoder = tiltakskoder,
                        navEnheter = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
            if (oppgavetyper.isEmpty() || oppgavetyper.any { it.kategori == Kategori.TILSKUDDBEHANDLING }) {
                addAll(
                    tilskuddBehandlingOppgaver(
                        tiltakskoder = tiltakskoder,
                        kostnadssteder = navEnheterForRegioner,
                        arrangorer = arrangorer,
                        ansatt = ansatt,
                    ),
                )
            }
        }

        return oppgaver.filter { oppgavetyper.isEmpty() || it.type in oppgavetyper }
    }

    private fun isEnkeltplassEnabled(): Boolean = features.isEnabled(FeatureToggle.TILTAKSADMINISTRASJON_ENKELTPLASS_FILTER)

    private fun QueryContext.tilsagnOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getTilsagnOppgaveData(arrangorer = arrangorer.ifEmpty { null })
            .asSequence()
            .filter { oppgave ->
                kostnadssteder.isEmpty() || oppgave.kostnadssted.nummer in kostnadssteder
            }
            .filter { tiltakskoder.isEmpty() || it.tiltakstype.tiltakskode in tiltakskoder }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.utbetalingLinjeOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getUtbetalingLinjeOppgaveData(
                kostnadssteder = kostnadssteder.ifEmpty { null },
                tiltakskoder = tiltakskoder.ifEmpty { null },
                arrangorer = arrangorer.ifEmpty { null },
            )
            .asSequence()
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.utbetalingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getUtbetalingOppgaveData(
                tiltakskoder = tiltakskoder.ifEmpty { null },
                arrangorer = arrangorer.ifEmpty { null },
            )
            .asSequence()
            .filter { utbetaling -> byKostnadssted(utbetaling, kostnadssteder) }
            .mapNotNull { toOppgave(it, ansatt) }
            .toList()
    }

    private fun QueryContext.avtaleOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        regioner: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getAvtaleManglerAdministratorOppgaveData(tiltakskoder, regioner, arrangorer.ifEmpty { null })
            .mapNotNull { it.toOppgave(ansatt) }
    }

    private fun QueryContext.gjennomforingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getGjennomforingManglerAdministratorOppgaveData(tiltakskoder, navEnheter, arrangorer.ifEmpty { null })
            .mapNotNull { it.toOppgave(ansatt) }
    }

    private fun QueryContext.enkeltplassOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        val tilGodkjenning = queries.oppgave
            .getEnkeltplassOppgaveData(
                tiltakskoder = tiltakskoder.ifEmpty { null },
                navEnheter = navEnheter.ifEmpty { null },
                arrangorer = arrangorer.ifEmpty { null },
            )
            .mapNotNull { it.toOppgave(ansatt) }

        val sattPaVent = queries.oppgave
            .getEnkeltplassSattPaVentOppgaveData(
                tiltakskoder = tiltakskoder.ifEmpty { null },
                navEnheter = navEnheter.ifEmpty { null },
                arrangorer = arrangorer.ifEmpty { null },
            )
            .mapNotNull { it.toOppgave(ansatt) }

        return tilGodkjenning + sattPaVent
    }

    private fun QueryContext.tilskuddBehandlingOppgaver(
        tiltakskoder: Set<Tiltakskode>,
        kostnadssteder: Set<NavEnhetNummer>,
        arrangorer: Set<UUID>,
        ansatt: NavAnsatt,
    ): List<Oppgave> {
        return queries.oppgave
            .getTilskuddBehandlingOppgaveData(
                tiltakskoder = tiltakskoder.ifEmpty { null },
                kostnadssteder = kostnadssteder.ifEmpty { null },
                arrangorer = arrangorer.ifEmpty { null },
            )
            .mapNotNull { toTilskuddBehandlingOppgave(it, ansatt) }
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

    private fun QueryContext.getNavEnheterOgKostnadsstederForRegioner(regioner: Set<NavEnhetNummer>): Set<NavEnhetNummer> {
        val kostnadssteder = queries.kostnadssted.getAll(regioner.toList())
            .flatMap { listOf(it.region.enhetsnummer, it.enhetsnummer) }
            .toSet()
        val navEnheter = regioner.flatMapTo(mutableSetOf()) { region ->
            queries.enhet.getAll(overordnetEnhet = region).map { it.enhetsnummer } + region
        }
        return kostnadssteder + navEnheter
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

    val opprettelse = queries.totrinnskontroll.getOrError(data.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
    val annullering = queries.totrinnskontroll.get(data.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
    val tilOppgjor = queries.totrinnskontroll.get(data.id, TotrinnskontrollType.TILSAGN_OPPGJOR)

    val title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing)
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
                createdAt = opprettelse.behandletTidspunkt.tilNorskLocalDateTime(),
                arrangor = data.arrangor,
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
            val besluttetTidspunkt = requireNotNull(opprettelse.besluttetTidspunkt)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSAGN_RETURNERT,
                navn = OppgaveType.TILSAGN_RETURNERT.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilsagnet ${data.bestillingsnummer} er returnert av beslutter",
                tiltakstype = tiltakstype,
                link = link,
                createdAt = besluttetTidspunkt.tilNorskLocalDateTime(),
                arrangor = data.arrangor,
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
                createdAt = annullering.behandletTidspunkt.tilNorskLocalDateTime(),
                arrangor = data.arrangor,
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
                createdAt = tilOppgjor.behandletTidspunkt.tilNorskLocalDateTime(),
                arrangor = data.arrangor,
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

private fun toOppgave(data: UtbetalingLinjeOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/${data.gjennomforing.id}/utbetalinger/${data.utbetalingId}",
    )

    return when (data.status) {
        UtbetalingLinjeStatus.TIL_ATTESTERING -> {
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
                arrangor = data.arrangor,
            ).takeIf {
                AdminUtbetalingService.tilgangTilHandling(
                    handling = UtbetalingLinjeHandling.ATTESTER,
                    ansatt = ansatt,
                    kostnadssted = data.kostnadssted.nummer,
                    behandletAv = data.opprettelse.behandletAv,
                )
            }
        }

        UtbetalingLinjeStatus.RETURNERT -> {
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
                arrangor = data.arrangor,
            ).takeIf {
                AdminUtbetalingService.tilgangTilHandling(
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
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.UTBETALT,
        UtbetalingStatusType.AVBRUTT,
        -> null

        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.RETURNERT,
        ->
            if (data.tilAvbrytelse) {
                tilAvbrytelseOppgave(data, ansatt)
            } else {
                null
            }

        UtbetalingStatusType.TIL_BEHANDLING ->
            if (data.tilAvbrytelse) {
                tilAvbrytelseOppgave(data, ansatt)
            } else {
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
                    arrangor = data.arrangor,
                ).takeIf { AdminUtbetalingService.tilgangTilHandling(UtbetalingHandling.SEND_TIL_ATTESTERING, ansatt) }
            }
    }
}

private fun tilAvbrytelseOppgave(data: UtbetalingOppgaveData, ansatt: NavAnsatt): Oppgave? = Oppgave(
    id = data.id,
    type = OppgaveType.UTBETALING_TIL_AVBRYTELSE,
    navn = OppgaveType.UTBETALING_TIL_AVBRYTELSE.navn,
    enhet = null,
    title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing),
    description = "Utbetaling for perioden ${data.periode.formatPeriode()} er sendt til avbrytelse",
    tiltakstype = data.tiltakstype,
    link = OppgaveLink(
        linkText = "Se utbetaling",
        link = "/gjennomforinger/${data.gjennomforing.id}/utbetalinger/${data.id}",
    ),
    createdAt = data.createdAt,
    arrangor = data.arrangor,
).takeIf { AdminUtbetalingService.tilgangTilHandling(UtbetalingHandling.GODKJENN_AVBRYTELSE, ansatt) }

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
    arrangor = arrangor,
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
    arrangor = arrangor,
).takeIf {
    GjennomforingDetaljerService.tilgangTilHandling(ansatt, GjennomforingHandling.REDIGER)
}

private fun EnkeltplassOppgaveData.toOppgave(ansatt: NavAnsatt): Oppgave? {
    return Oppgave(
        id = gjennomforing.id,
        type = OppgaveType.ENKELTPLASS_TIL_GODKJENNING,
        navn = OppgaveType.ENKELTPLASS_TIL_GODKJENNING.navn,
        enhet = ansvarligEnhet,
        title = getOkonomiOppgaveTitle(tiltakstype, gjennomforing),
        description = "Enkeltplassen er sendt til godkjenning",
        tiltakstype = tiltakstype,
        link = OppgaveLink(
            linkText = "Se enkeltplass",
            link = "/gjennomforinger/${gjennomforing.id}",
        ),
        createdAt = behandletTidspunkt,
        arrangor = arrangor,
    ).takeIf {
        behandletAv != ansatt.navIdent && GjennomforingDetaljerService.tilgangTilHandling(
            ansatt,
            GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
            setOf(ansvarligEnhet.nummer),
        )
    }
}

private fun EnkeltplassSattPaVentOppgaveData.toOppgave(ansatt: NavAnsatt): Oppgave? {
    return Oppgave(
        id = gjennomforing.id,
        type = OppgaveType.ENKELTPLASS_SATT_PA_VENT,
        navn = OppgaveType.ENKELTPLASS_SATT_PA_VENT.navn,
        enhet = ansvarligEnhet,
        title = getOkonomiOppgaveTitle(tiltakstype, gjennomforing),
        description = "Enkeltplassen er satt på vent av beslutter",
        tiltakstype = tiltakstype,
        link = OppgaveLink(
            linkText = "Se enkeltplass",
            link = "/gjennomforinger/${gjennomforing.id}",
        ),
        createdAt = besluttetTidspunkt,
        arrangor = arrangor,
    ).takeIf {
        GjennomforingDetaljerService.tilgangTilHandling(ansatt, GjennomforingHandling.OPPRETT_UTBETALING)
    }
}

private fun toTilskuddBehandlingOppgave(data: TilskuddBehandlingOppgaveData, ansatt: NavAnsatt): Oppgave? {
    val link = OppgaveLink(
        linkText = "Se tilskuddsbehandling",
        link = "/gjennomforinger/${data.gjennomforing.id}/tilskudd-behandling/${data.id}",
    )

    val title = getOkonomiOppgaveTitle(data.tiltakstype, data.gjennomforing)

    return when (data.status) {
        TilskuddBehandlingStatus.TIL_ATTESTERING -> {
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSKUDDBEHANDLING_TIL_ATTESTERING,
                navn = OppgaveType.TILSKUDDBEHANDLING_TIL_ATTESTERING.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilskuddsbehandling for perioden ${data.periode.formatPeriode()} er sendt til attestering",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = data.opprettelse.behandletTidspunkt,
                arrangor = data.arrangor,
            ).takeIf {
                data.opprettelse.behandletAv != ansatt.navIdent
            }
        }

        TilskuddBehandlingStatus.RETURNERT -> {
            requireNotNull(data.opprettelse.besluttetTidspunkt)
            Oppgave(
                id = data.id,
                type = OppgaveType.TILSKUDDBEHANDLING_RETURNERT,
                navn = OppgaveType.TILSKUDDBEHANDLING_RETURNERT.navn,
                enhet = data.kostnadssted,
                title = title,
                description = "Tilskuddsbehandling for perioden ${data.periode.formatPeriode()} er returnert av attestant",
                tiltakstype = data.tiltakstype,
                link = link,
                createdAt = data.opprettelse.besluttetTidspunkt,
                arrangor = data.arrangor,
            )
        }

        TilskuddBehandlingStatus.FERDIG_BEHANDLET -> null
    }
}

private fun getOkonomiOppgaveTitle(tiltakstype: OppgaveTiltakstype, gjennomforing: OppgaveGjennomforing): String {
    return when (gjennomforing) {
        is OppgaveGjennomforing.Gruppetiltak -> "${gjennomforing.navn} (${gjennomforing.lopenummer})"
        is OppgaveGjennomforing.Enkeltplass -> "${tiltakstype.navn} (${gjennomforing.lopenummer})"
    }
}
