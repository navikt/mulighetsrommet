package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptySetOf
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.journalpost.ForventetBruker
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusAarsak
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.validation
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AdminUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val journalpostValidator: JournalpostValidator,
    private val featureToggleService: FeatureToggleService,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend fun opprettUtbetaling(
        request: UtbetalingRequest,
        agent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> {
        val ctx = db.session {
            val gjennomforingId = when (val korrigerer = request.korrigererUtbetaling) {
                null -> request.gjennomforingId
                else -> queries.utbetaling.get(korrigerer)?.gjennomforing?.id
            } ?: return UtbetalingError.Valideringsfeil(
                FieldError.of("Utbetaling som skal korrigeres eksisterer ikke").nel(),
            ).left()

            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(gjennomforingId)
            if (!kanOppretteUtbetaling(agent, gjennomforing)) {
                return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
            }
            hentValideringsContext(arrangorId = gjennomforing.arrangor.id, korrigertUtbetalingFinnes = true)
        }

        val opprett = UtbetalingValidator.validateUpsertUtbetaling(request, ctx)
            .getOrElse { return UtbetalingError.Valideringsfeil(it).left() }

        return db.transaction {
            utbetalingService.opprettUtbetaling(opprett, agent).mapLeft(UtbetalingError::Valideringsfeil)
        }
    }

    /**
     * Arrangøren hentes fra den lagrede utbetalingen, ikke fra gjennomføringen i requesten, slik at
     * journalposten bare valideres mot en arrangør saksbehandler har tilgang til.
     */
    suspend fun redigerUtbetaling(
        request: UtbetalingRequest,
        agent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> {
        val ctx = db.session {
            val utbetaling = queries.utbetaling.getOrError(request.id)
            if (!erSaksbehandler(agent, utbetaling)) {
                return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
            }
            hentValideringsContext(
                arrangorId = utbetaling.arrangor.id,
                korrigertUtbetalingFinnes = request.korrigererUtbetaling
                    ?.let { queries.utbetaling.get(it) != null }
                    ?: true,
            )
        }

        val rediger = UtbetalingValidator.validateUpsertUtbetaling(request, ctx)
            .getOrElse { return UtbetalingError.Valideringsfeil(it).left() }

        return db.transaction {
            val utbetaling = queries.utbetaling.getAndAcquireLock(rediger.id)
            if (!erSaksbehandler(agent, utbetaling)) {
                return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
            }

            if (!utbetaling.kanRedigeres()) {
                return UtbetalingError.Valideringsfeil(FieldError.of("Utbetalingen kan ikke redigeres").nel()).left()
            }

            val gjennomforingId = getGjennomforingId(rediger)
            if (gjennomforingId != utbetaling.gjennomforing.id) {
                return UtbetalingError.Valideringsfeil(
                    FieldError.of("Utbetalingen kan ikke flyttes til en annen gjennomføring").nel(),
                ).left()
            }

            utbetalingService.redigerUtbetaling(rediger, agent).mapLeft(UtbetalingError::Valideringsfeil)
        }
    }

    private fun QueryContext.hentValideringsContext(
        arrangorId: UUID,
        korrigertUtbetalingFinnes: Boolean,
    ): UtbetalingValidator.Ctx {
        val arrangor = db.session { repository.arrangor.get(arrangorId) }
        val forventetBruker = ForventetBruker.Bedrift(organisasjonsnummer = arrangor.organisasjonsnummer)

        val journalpostValidatorFunc = UtbetalingValidator.createJournalpostValidator(
            forventetBruker = forventetBruker,
            journalpostValidator = journalpostValidator,
            valideringEnabled = featureToggleService.isEnabled(FeatureToggle.TILTAKSADMINISTRASJON_JOURNALPOST_VALIDERING),
        )

        return UtbetalingValidator.Ctx(
            arrangor = repository.arrangor.get(arrangorId),
            korrigertUtbetalingFinnes = korrigertUtbetalingFinnes,
            journalpostValidator = journalpostValidatorFunc,
            today = today(),
        )
    }

    fun sendTilGodkjenning(
        opprett: OpprettUtbetalingLinjer,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(opprett.utbetalingId)
        if (!erSaksbehandler(navIdent, utbetaling)) {
            return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
        }

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
                validate(tilsagn.gjennomforing.id == utbetaling.gjennomforing.id) {
                    FieldError("/utbetalingLinjer/$index/tilsagnId", "Tilsagnet tilhører en annen gjennomføring")
                }
                val previous = queries.utbetalingLinje.get(linje.id)
                validate(previous == null || previous.utbetalingId == utbetaling.id) {
                    FieldError("/utbetalingLinjer/$index/id", "Utbetalingslinjen tilhører en annen utbetaling")
                }
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
            utbetalingService.sendTilGodkjenning(utbetaling.id, opprett.linjer, navIdent)
        }.mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun sendTilAvbrytelse(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, utbetaling)) {
            return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
        }

        utbetalingService.sendTilAvbrytelse(
            id = id,
            agent = navIdent,
            aarsaker = request.aarsaker.map { it.name },
            begrunnelse = request.begrunnelse,
        ).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun godkjennAvbrytelse(id: UUID, navIdent: NavIdent): Either<UtbetalingError, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, utbetaling)) {
            return UtbetalingError.ManglerTilgang("Du mangler tilgang til å godkjenne avbrytelsen").left()
        }

        return utbetalingService.godkjennAvbrytelse(id, navIdent).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun avslaAvbrytelse(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, utbetaling)) {
            return UtbetalingError.ManglerTilgang("Du mangler tilgang til å avslå avbrytelsen").left()
        }

        return utbetalingService.avslaAvbrytelse(
            id = id,
            besluttetAv = navIdent,
            aarsaker = request.aarsaker.map { it.name },
            begrunnelse = request.begrunnelse,
        ).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun godkjennUtbetalingLinje(
        id: UUID,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        val linje = queries.utbetalingLinje.getOrError(id)
        val utbetaling = queries.utbetaling.getAndAcquireLock(linje.utbetalingId)
        val tilsagn = queries.tilsagn.getAndAcquireLock(linje.tilsagnId)
        if (!erAttestant(navIdent, utbetaling, tilsagn)) {
            return UtbetalingError.ManglerTilgang(
                "Du kan ikke godkjenne utbetalingen fordi du ikke er beslutter ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})",
            ).left()
        }

        utbetalingService.godkjennUtbetalingLinje(id, navIdent).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        begrunnelse: String?,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        val linje = queries.utbetalingLinje.getOrError(id)
        val utbetaling = queries.utbetaling.getAndAcquireLock(linje.utbetalingId)
        val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId)
        if (!erSaksbehandler(navIdent, utbetaling) && !erAttestant(navIdent, utbetaling, tilsagn)) {
            return UtbetalingError.ManglerTilgang("Du kan ikke returnere utbetalingen fordi du mangler tilgang").left()
        }

        utbetalingService.returnerUtbetalingLinje(
            id = id,
            aarsaker = aarsaker,
            begrunnelse = begrunnelse,
            agent = navIdent,
        ).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun slettUtbetaling(id: UUID, navIdent: NavIdent): Either<UtbetalingError, Unit> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, utbetaling)) {
            return UtbetalingError.ManglerTilgang("Du mangler saksbehandlertilgang til utbetalingen").left()
        }

        utbetalingService.slettUtbetaling(id).mapLeft(UtbetalingError::Valideringsfeil)
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = db.transaction {
        utbetalingService.oppdaterFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)
    }

    private fun QueryContext.getGjennomforingId(upsert: UpsertUtbetaling): UUID? = when (upsert) {
        is UpsertUtbetaling.Anskaffelse -> upsert.gjennomforingId
        is UpsertUtbetaling.Generering -> upsert.gjennomforingId
        is UpsertUtbetaling.Innsending -> upsert.gjennomforingId
        is UpsertUtbetaling.Korreksjon -> queries.utbetaling.get(upsert.korreksjonGjelderUtbetalingId)?.gjennomforing?.id
    }

    private fun QueryContext.kanOppretteUtbetaling(
        navIdent: NavIdent,
        gjennomforing: GjennomforingTiltaksadministrasjon,
    ): Boolean {
        val type = when (gjennomforing) {
            is GjennomforingAvtale -> GjennomforingType.AVTALE
            is GjennomforingEnkeltplass -> GjennomforingType.ENKELTPLASS
        }
        val ansatt = queries.ansatt.getOrError(navIdent)
        return OkonomiAuthorization.erSaksbehandler(ansatt, type)
    }

    private fun QueryContext.erSaksbehandler(navIdent: NavIdent, utbetaling: Utbetaling): Boolean {
        val ansatt = queries.ansatt.getOrError(navIdent)
        return OkonomiAuthorization.erSaksbehandler(ansatt, utbetaling.gjennomforing.type)
    }

    private fun QueryContext.erAttestant(
        navIdent: NavIdent,
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
    ): Boolean {
        val ansatt = queries.ansatt.getOrError(navIdent)
        val kontekst = OkonomiBeslutningContext(
            gjennomforingType = utbetaling.gjennomforing.type,
            kostnadssteder = nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
        )
        return OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst)
    }
}
