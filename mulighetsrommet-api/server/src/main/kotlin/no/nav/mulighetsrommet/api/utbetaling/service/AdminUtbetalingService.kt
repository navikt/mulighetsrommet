package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDeltakerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingDetaljerDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeStatusDto
import no.nav.mulighetsrommet.api.utbetaling.model.AttesterUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.AvbrytUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.AvslaAvbrytUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.GodkjennAvbrytUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.ReturnerUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.hentDeltakerAdvarslerForUtbetaling
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.validation.Validated
import no.nav.mulighetsrommet.validation.validation
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.util.UUID

class AdminUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val personaliaService: PersonaliaService,
) {
    fun getUtbetalingDetaljer(id: UUID, navIdent: NavIdent): UtbetalingDetaljerDto = db.session {
        val utbetaling = queries.utbetaling.getOrError(id)
        val linjer = queries.utbetalingLinje.getByUtbetalingId(id)
        val avbrytelse = utbetaling.avbrytelse?.totrinnskontroll?.let {
            queries.totrinnskontroll.getDtoByIdOrError(it.id)
        }
        val dto = UtbetalingDto.fromUtbetaling(
            utbetaling = utbetaling,
            linjer = linjer,
            avbrytelse = avbrytelse,
        )

        val ansatt = queries.ansatt.getOrError(navIdent)
        val handlinger = utbetalingHandlinger(utbetaling, ansatt, dto.avbrytelse)

        return UtbetalingDetaljerDto(utbetaling = dto, handlinger = handlinger)
    }

    suspend fun getUtbetalingLinjer(
        id: UUID,
        navIdent: NavIdent,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): List<UtbetalingLinjeDto> = db.session {
        val utbetaling = queries.utbetaling.getOrError(id)
        val ansatt = queries.ansatt.getOrError(navIdent)

        val linjer = queries.utbetalingLinje.getByUtbetalingId(id).map { linje ->
            val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId)

            val opprettelse = queries.totrinnskontroll
                .getDtoOrError(linje.id, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE)

            val personalia = personaliaService.getPersonalia(
                tilsagn.deltakere.map { it.deltakerId },
                onBehalfOf,
            )
            val deltakere = tilsagn.deltakere.map {
                TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
            }
            UtbetalingLinjeDto(
                id = linje.id,
                gjorOppTilsagn = linje.gjorOppTilsagn,
                pris = linje.pris,
                status = UtbetalingLinjeStatusDto.fromUtbetalingLinjeStatus(linje.status),
                tilsagn = TilsagnDto.from(tilsagn),
                deltakere = deltakere,
                opprettelse = opprettelse,
                handlinger = linjeHandlinger(
                    linje,
                    opprettelse.behandletAv.agent,
                    tilsagn.kostnadssted.enhetsnummer,
                    ansatt,
                ),
            )
        }

        val nyeLinjer = queries.tilsagn
            .getAll(
                statuser = listOf(TilsagnStatus.GODKJENT),
                gjennomforingId = utbetaling.gjennomforing.id,
                periodeIntersectsWith = utbetaling.periode,
                typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                valuta = utbetaling.valuta,
            )
            .filter { tilsagn -> linjer.none { it.tilsagn.id == tilsagn.id } }
            .map { tilsagn ->
                val deltakerIds = tilsagn.deltakere.map { it.deltakerId }
                val personalia = personaliaService.getPersonalia(
                    deltakerIds,
                    onBehalfOf,
                )
                val deltakere = tilsagn.deltakere.map {
                    TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                }

                UtbetalingLinjeDto(
                    id = UUID.randomUUID(),
                    tilsagn = TilsagnDto.from(tilsagn),
                    deltakere = deltakere,
                    status = null,
                    pris = ValutaBelop(0, utbetaling.valuta),
                    gjorOppTilsagn = false,
                    opprettelse = null,
                    handlinger = emptySet(),
                )
            }

        return (linjer + nyeLinjer).sortedBy { it.tilsagn.bestillingsnummer }
    }

    suspend fun opprettUtbetaling(
        opprett: UpsertUtbetaling,
        agent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        when (opprett) {
            is UpsertUtbetaling.Anskaffelse if opprett.journalpostId == null -> {
                val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(opprett.gjennomforingId)
                val arrangor = repository.arrangor.get(gjennomforing.arrangor.id)
                if (arrangor is Arrangor.Norsk) {
                    return FieldError.of("Journalpost-ID er påkrevd", UpsertUtbetaling.Anskaffelse::journalpostId)
                        .nel()
                        .left()
                }
            }

            is UpsertUtbetaling.Korreksjon -> {
                if (queries.utbetaling.get(opprett.korreksjonGjelderUtbetalingId) == null) {
                    return FieldError.of("Utbetaling som skal korrigeres eksisterer ikke").nel().left()
                }
            }

            else -> Unit
        }

        utbetalingService.opprettUtbetaling(opprett, agent)
    }

    suspend fun redigerUtbetaling(
        rediger: UpsertUtbetaling,
        agent: NavIdent,
    ): Validated<Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(rediger.id)

        if (!kanRedigeres(utbetaling)) {
            return FieldError.of("Utbetalingen kan ikke redigeres").nel().left()
        }

        utbetalingService.redigerUtbetaling(rediger, agent)
    }

    fun sendTilAttestering(
        opprett: OpprettUtbetalingLinjer,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(opprett.utbetalingId)
        val tilsagnByLinjeId = opprett.linjer.associate { it.id to queries.tilsagn.getAndAquireLock(it.tilsagnId) }

        validation {
            validate(utbetaling.erTilBehandling()) {
                FieldError.of("Utbetaling kan bare endres når den er til behandling")
            }

            val totalBelopUtbetales = opprett.linjer.sumOf { it.pris.belop }.withValuta(utbetaling.valuta)
            validate(totalBelopUtbetales.belop > 0) {
                FieldError.of("Totalt beløp må være større enn 0")
            }
            validate(totalBelopUtbetales <= utbetaling.beregning.output.pris) {
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp")
            }
            if (totalBelopUtbetales < utbetaling.beregning.output.pris && opprett.begrunnelseMindreBetalt.isNullOrBlank()) {
                error { FieldError.of("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp") }
            }

            opprett.linjer.forEachIndexed { index, linje ->
                val tilsagn = tilsagnByLinjeId.getValue(linje.id)
                validate(linje.pris <= tilsagn.gjenstaendeBelop()) {
                    FieldError(
                        "/utbetalingLinjer/$index/tilsagnId",
                        "Beløp overstiger gjenstående beløp på tilsagn. For å utbetale hele beløpet må dere først opprette og godkjenne et ekstratilsagn",
                    )
                }
                validate(tilsagn.status == TilsagnStatus.GODKJENT) {
                    FieldError(
                        "/utbetalingLinjer/$index/tilsagnId",
                        "Tilsagnet har status ${tilsagn.status.beskrivelse} og kan ikke benyttes, linjen må fjernes",
                    )
                }
            }
        }.flatMap {
            queries.utbetaling.setBegrunnelseMindreBetalt(utbetaling.id, opprett.begrunnelseMindreBetalt)
            utbetalingService.sendTilAttestering(utbetaling.id, opprett.linjer, navIdent)
        }
    }

    fun sendTilAvbrytelse(
        command: AvbrytUtbetaling,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.sendTilAvbrytelse(command)
    }

    fun godkjennAvbrytelse(
        command: GodkjennAvbrytUtbetaling,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        return utbetalingService.godkjennAvbrytelse(command)
    }

    fun avslaAvbrytelse(
        command: AvslaAvbrytUtbetaling,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        return utbetalingService.avslaAvbrytelse(command)
    }

    fun godkjennUtbetalingLinje(
        command: AttesterUtbetalingLinje,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.attesterUtbetalingLinje(command)
    }

    fun returnerUtbetalingLinje(
        command: ReturnerUtbetalingLinje,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.returnerUtbetalingLinje(command)
    }

    fun slettKorreksjon(id: UUID): Either<List<FieldError>, Unit> = db.transaction {
        utbetalingService.slettKorreksjon(id)
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = db.transaction {
        utbetalingService.oppdaterFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)
    }

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.transaction {
        hentDeltakerAdvarslerForUtbetaling(
            status = utbetaling.status,
            gjennomforingId = utbetaling.gjennomforing.id,
            periode = utbetaling.periode,
            beregning = utbetaling.beregning,
        )
    }

    companion object {
        fun utbetalingHandlinger(
            utbetaling: Utbetaling,
            ansatt: NavAnsatt,
            tilAvbrytelse: TotrinnskontrollDto?,
        ) = setOfNotNull(
            UtbetalingHandling.SEND_TIL_ATTESTERING.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.SLETT.takeIf { utbetaling.erTilBehandling() && utbetaling.erKorreksjon() },
            UtbetalingHandling.OPPRETT_KORREKSJON.takeIf { utbetaling.erFerdigBehandlet() && !utbetaling.erKorreksjon() },
            UtbetalingHandling.REDIGER.takeIf { kanRedigeres(utbetaling) },
            UtbetalingHandling.HENT_GODKJENTE_TILSAGN.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.OPPRETT_TILSAGN.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.SEND_TIL_AVBRYTELSE.takeIf { utbetaling.kanSettesTilAvbrytelse() },
            UtbetalingHandling.GODKJENN_AVBRYTELSE.takeIf { kanGodkjenneAvbrytelse(ansatt, tilAvbrytelse) },
            UtbetalingHandling.AVSLA_AVBRYTELSE.takeIf { kanAvslaAvbrytelse(tilAvbrytelse) },
        )
            .filter { handling ->
                tilgangTilHandling(handling, ansatt)
            }
            .toSet()

        private fun kanGodkjenneAvbrytelse(ansatt: NavAnsatt, tilAvbrytelse: TotrinnskontrollDto?) = when (tilAvbrytelse) {
            is TotrinnskontrollDto.TilBeslutning ->
                tilAvbrytelse.behandletAv.agent != ansatt.navIdent

            is TotrinnskontrollDto.Besluttet,
            null,
            -> false
        }

        private fun kanAvslaAvbrytelse(tilAvbrytelse: TotrinnskontrollDto?) = when (tilAvbrytelse) {
            is TotrinnskontrollDto.TilBeslutning ->
                true

            is TotrinnskontrollDto.Besluttet,
            null,
            -> false
        }

        fun linjeHandlinger(
            linje: UtbetalingLinje,
            behandletAv: Agent,
            kostnadssted: NavEnhetNummer,
            ansatt: NavAnsatt,
        ): Set<UtbetalingLinjeHandling> {
            return setOfNotNull(
                UtbetalingLinjeHandling.ATTESTER.takeIf { linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING },
                UtbetalingLinjeHandling.RETURNER.takeIf { linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING },
            )
                .filter {
                    tilgangTilHandling(
                        handling = it,
                        ansatt = ansatt,
                        kostnadssted = kostnadssted,
                        behandletAv = behandletAv,
                    )
                }
                .toSet()
        }

        fun tilgangTilHandling(handling: UtbetalingHandling, ansatt: NavAnsatt): Boolean {
            val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
            return when (handling) {
                UtbetalingHandling.OPPRETT_KORREKSJON -> saksbehandlerOkonomi
                UtbetalingHandling.REDIGER -> saksbehandlerOkonomi
                UtbetalingHandling.SEND_TIL_ATTESTERING -> saksbehandlerOkonomi
                UtbetalingHandling.SLETT -> saksbehandlerOkonomi
                UtbetalingHandling.HENT_GODKJENTE_TILSAGN -> saksbehandlerOkonomi
                UtbetalingHandling.OPPRETT_TILSAGN -> saksbehandlerOkonomi
                UtbetalingHandling.SEND_TIL_AVBRYTELSE -> saksbehandlerOkonomi
                UtbetalingHandling.GODKJENN_AVBRYTELSE -> saksbehandlerOkonomi
                UtbetalingHandling.AVSLA_AVBRYTELSE -> saksbehandlerOkonomi
            }
        }

        fun tilgangTilHandling(
            handling: UtbetalingLinjeHandling,
            ansatt: NavAnsatt,
            kostnadssted: NavEnhetNummer,
            behandletAv: Agent,
        ): Boolean {
            val erBeslutter = ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted))
            val erSaksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                UtbetalingLinjeHandling.SEND_TIL_ATTESTERING -> erSaksbehandler
                UtbetalingLinjeHandling.ATTESTER -> erBeslutter && behandletAv != ansatt.navIdent
                UtbetalingLinjeHandling.RETURNER -> erBeslutter || erSaksbehandler
            }
        }
    }
}

private fun kanRedigeres(utbetaling: Utbetaling): Boolean = utbetaling.erTilBehandling() && !utbetaling.erInnsending()
