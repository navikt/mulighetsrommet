package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusAarsak
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.validation.Validated
import no.nav.mulighetsrommet.validation.validation
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.util.UUID

class AdminUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) {
    suspend fun opprettUtbetaling(
        opprett: UpsertUtbetaling,
        agent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        upsertValidation(opprett).onLeft { return it.left() }

        utbetalingService.opprettUtbetaling(opprett, agent)
    }

    suspend fun redigerUtbetaling(
        rediger: UpsertUtbetaling,
        agent: NavIdent,
    ): Validated<Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(rediger.id)

        if (!kanRedigeres(utbetaling)) {
            return FieldError.of("Utbetalingen kan ikke redigeres").nel().left()
        }
        upsertValidation(rediger).onLeft { return it.left() }

        utbetalingService.redigerUtbetaling(rediger, agent)
    }

    fun TransactionalQueryContext.upsertValidation(utbetaling: UpsertUtbetaling): Either<List<FieldError>, Unit> = when (utbetaling) {
        is UpsertUtbetaling.Anskaffelse if (utbetaling.journalpostId == null || utbetaling.utbetalingsDato == null) -> {
            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforingId)
            val arrangor = repository.arrangor.get(gjennomforing.arrangor.id)
            if (arrangor is Arrangor.Norsk) {
                listOfNotNull(
                    FieldError.of("Journalpost-ID er påkrevd", UpsertUtbetaling.Anskaffelse::journalpostId)
                        .takeIf { utbetaling.journalpostId == null },
                    FieldError.of("Utbetalingsdato er påkrevd", UpsertUtbetaling.Anskaffelse::utbetalingsDato)
                        .takeIf { utbetaling.utbetalingsDato == null },
                )
                    .left()
            } else {
                Unit.right()
            }
        }

        is UpsertUtbetaling.Korreksjon ->
            if (queries.utbetaling.get(utbetaling.korreksjonGjelderUtbetalingId) == null) {
                FieldError.of("Utbetaling som skal korrigeres eksisterer ikke").nel().left()
            } else {
                Unit.right()
            }

        else -> Unit.right()
    }

    fun sendTilAttestering(
        opprett: OpprettUtbetalingLinjer,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(opprett.utbetalingId)
        val tilsagnByLinjeId = opprett.linjer.associate { it.id to queries.tilsagn.getAndAcquireLock(it.tilsagnId) }

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
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.sendTilAvbrytelse(
            id = id,
            agent = navIdent,
            aarsaker = request.aarsaker.map { it.name },
            begrunnelse = request.begrunnelse,
        )
    }

    fun godkjennAvbrytelse(id: UUID, navIdent: NavIdent): Either<List<FieldError>, Utbetaling> = db.transaction {
        return utbetalingService.godkjennAvbrytelse(id, navIdent)
    }

    fun avslaAvbrytelse(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        return utbetalingService.avslaAvbrytelse(
            id = id,
            besluttetAv = navIdent,
            aarsaker = request.aarsaker.map { it.name },
            begrunnelse = request.begrunnelse,
        )
    }

    fun godkjennUtbetalingLinje(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.attesterUtbetalingLinje(id, navIdent)
    }

    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        begrunnelse: String?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.returnerUtbetalingLinje(id, aarsaker, begrunnelse, navIdent)
    }

    fun slettUtbetaling(id: UUID): Either<List<FieldError>, Unit> = db.transaction {
        utbetalingService.slettUtbetaling(id)
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = db.transaction {
        utbetalingService.oppdaterFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)
    }

    companion object {
        fun utbetalingHandlinger(
            utbetaling: Utbetaling,
            ansatt: NavAnsatt,
            tilAvbrytelse: TotrinnskontrollDto?,
        ): Set<UtbetalingHandling> {
            val erEnkeltplass = utbetaling.gjennomforing.type == GjennomforingType.ENKELTPLASS
            return setOfNotNull(
                UtbetalingHandling.SEND_TIL_ATTESTERING.takeIf { utbetaling.erTilBehandling() },
                UtbetalingHandling.SLETT.takeIf {
                    utbetaling.erTilBehandling() && (utbetaling.erKorreksjon() || erEnkeltplass)
                },
                UtbetalingHandling.OPPRETT_KORREKSJON.takeIf { utbetaling.erFerdigBehandlet() && !utbetaling.erKorreksjon() },
                UtbetalingHandling.REDIGER.takeIf { kanRedigeres(utbetaling) },
                UtbetalingHandling.HENT_GODKJENTE_TILSAGN.takeIf { utbetaling.erTilBehandling() },
                UtbetalingHandling.OPPRETT_TILSAGN.takeIf { utbetaling.erTilBehandling() },
                UtbetalingHandling.SEND_TIL_AVBRYTELSE.takeIf { !erEnkeltplass && utbetaling.kanSettesTilAvbrytelse() },
                UtbetalingHandling.GODKJENN_AVBRYTELSE.takeIf { kanGodkjenneAvbrytelse(ansatt, tilAvbrytelse) },
                UtbetalingHandling.AVSLA_AVBRYTELSE.takeIf { kanAvslaAvbrytelse(tilAvbrytelse) },
            )
                .filter { handling ->
                    tilgangTilHandling(handling, ansatt, utbetaling.gjennomforing.type)
                }
                .toSet()
        }

        private fun kanGodkjenneAvbrytelse(ansatt: NavAnsatt, tilAvbrytelse: TotrinnskontrollDto?) = when (tilAvbrytelse) {
            is TotrinnskontrollDto.TilBeslutning -> tilAvbrytelse.behandling.utfortAv.agent != ansatt.navIdent
            is TotrinnskontrollDto.Besluttet, null -> false
        }

        private fun kanAvslaAvbrytelse(tilAvbrytelse: TotrinnskontrollDto?) = when (tilAvbrytelse) {
            is TotrinnskontrollDto.TilBeslutning -> true
            is TotrinnskontrollDto.Besluttet, null -> false
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

        fun tilgangTilHandling(
            handling: UtbetalingHandling,
            ansatt: NavAnsatt,
            gjennomforingType: GjennomforingType,
        ): Boolean {
            val erSaksbehandler = OkonomiAuthorization.erSaksbehandler(ansatt, gjennomforingType)
            return when (handling) {
                UtbetalingHandling.OPPRETT_KORREKSJON -> erSaksbehandler
                UtbetalingHandling.REDIGER -> erSaksbehandler
                UtbetalingHandling.SEND_TIL_ATTESTERING -> erSaksbehandler
                UtbetalingHandling.SLETT -> erSaksbehandler
                UtbetalingHandling.HENT_GODKJENTE_TILSAGN -> erSaksbehandler
                UtbetalingHandling.OPPRETT_TILSAGN -> erSaksbehandler
                UtbetalingHandling.SEND_TIL_AVBRYTELSE -> erSaksbehandler
                UtbetalingHandling.GODKJENN_AVBRYTELSE -> erSaksbehandler
                UtbetalingHandling.AVSLA_AVBRYTELSE -> erSaksbehandler
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
