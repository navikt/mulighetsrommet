package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptySetOf
import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.ensureNotNull
import arrow.core.raise.context.raise
import arrow.core.right
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.journalpost.ForventetBruker
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafDokument
import no.nav.mulighetsrommet.api.clients.saf.SafDokumentvariant
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.gjennomforing.statusSperrerOkonomi
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorizationError
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingVedleggDto
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusAarsak
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AdminUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val journalpostValidator: JournalpostValidator,
    private val safClient: SafClient,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend fun opprettUtbetaling(
        request: UtbetalingRequest,
        agent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = either {
        val ctx = db.session {
            val gjennomforingId = when (val korrigerer = request.korrigererUtbetaling) {
                null -> request.gjennomforingId
                else -> queries.utbetaling.get(korrigerer)?.gjennomforing?.id
            }
            ensureNotNull(gjennomforingId) {
                UtbetalingError.Valideringsfeil(
                    FieldError.of("Utbetaling som skal korrigeres eksisterer ikke").nel(),
                )
            }

            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(gjennomforingId)
            kanOppretteUtbetaling(agent, gjennomforing).bind()

            gjennomforing.statusSperrerOkonomi()?.let { status ->
                raise(
                    UtbetalingError.Valideringsfeil(
                        FieldError.of("Kan ikke opprette utbetaling fordi deltakelsen har status ${status.type.beskrivelse}").nel(),
                    ),
                )
            }
            hentValideringsContext(arrangorId = gjennomforing.arrangor.id, korrigertUtbetalingFinnes = true, gjennomforingOppstartsType = gjennomforing.oppstart)
        }

        val opprett = UtbetalingValidator.validateUpsertUtbetaling(request, ctx)
            .mapLeft { UtbetalingError.Valideringsfeil(it) }
            .bind()

        db.transaction {
            utbetalingService.opprettUtbetaling(opprett, agent).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    /**
     * Arrangøren hentes fra den lagrede utbetalingen, ikke fra gjennomføringen i requesten, slik at
     * journalposten bare valideres mot en arrangør saksbehandler har tilgang til.
     */
    suspend fun redigerUtbetaling(
        request: UtbetalingRequest,
        agent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = either {
        val ctx = db.session {
            val utbetaling = queries.utbetaling.getOrError(request.id)
            ensureErSaksbehandler(agent, utbetaling)

            val gjennomforing =
                queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforing.id)
            hentValideringsContext(
                arrangorId = utbetaling.arrangor.id,
                korrigertUtbetalingFinnes = request.korrigererUtbetaling
                    ?.let { queries.utbetaling.get(it) != null }
                    ?: true,
                gjennomforingOppstartsType = gjennomforing.oppstart,
            )
        }

        val rediger = UtbetalingValidator.validateUpsertUtbetaling(request, ctx).mapLeft { UtbetalingError.Valideringsfeil(it) }.bind()

        db.transaction {
            val utbetaling = queries.utbetaling.getAndAcquireLock(rediger.id)
            ensureErSaksbehandler(agent, utbetaling)

            ensure(utbetaling.kanRedigeres()) {
                UtbetalingError.Valideringsfeil(FieldError.of("Utbetalingen kan ikke redigeres").nel())
            }

            val gjennomforingId = getGjennomforingId(rediger)
            ensure(gjennomforingId == utbetaling.gjennomforing.id) {
                UtbetalingError.Valideringsfeil(
                    FieldError.of("Utbetalingen kan ikke flyttes til en annen gjennomføring").nel(),
                )
            }

            utbetalingService.redigerUtbetaling(rediger, agent).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    private fun QueryContext.hentValideringsContext(
        arrangorId: UUID,
        korrigertUtbetalingFinnes: Boolean,
        gjennomforingOppstartsType: GjennomforingOppstartstype,
    ): UtbetalingValidator.Ctx {
        val arrangor = db.session { repository.arrangor.get(arrangorId) }
        val forventetBruker = ForventetBruker.Bedrift(organisasjonsnummer = arrangor.organisasjonsnummer)

        val journalpostValidatorFunc = UtbetalingValidator.createJournalpostValidator(
            forventetBruker = forventetBruker,
            journalpostValidator = journalpostValidator,
        )

        return UtbetalingValidator.Ctx(
            arrangor = repository.arrangor.get(arrangorId),
            korrigertUtbetalingFinnes = korrigertUtbetalingFinnes,
            journalpostValidator = journalpostValidatorFunc,
            gjennomforingOppstartsType = gjennomforingOppstartsType,
            today = today(),
        )
    }

    fun sendTilGodkjenning(
        opprett: OpprettUtbetalingLinjer,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val utbetaling = queries.utbetaling.getAndAcquireLock(opprett.utbetalingId)
            ensureErSaksbehandler(navIdent, utbetaling)

            val ctx = UtbetalingValidator.SendTilGodkjenningCtx(
                utbetaling = utbetaling,
                tilsagnByLinjeId = opprett.linjer.associate { it.id to queries.tilsagn.getAndAcquireLock(it.tilsagnId) },
                eksisterendeUtbetalingIdByLinjeId = opprett.linjer
                    .mapNotNull { linje -> queries.utbetalingLinje.get(linje.id)?.let { linje.id to it.utbetalingId } }
                    .toMap(),
            )

            UtbetalingValidator.validerSendTilGodkjenning(opprett, ctx).flatMap {
                queries.utbetaling.setBegrunnelseMindreBetalt(utbetaling.id, opprett.begrunnelseMindreBetalt)
                utbetalingService.sendTilGodkjenning(utbetaling.id, opprett.linjer, navIdent)
            }.mapLeft(UtbetalingError::Valideringsfeil)
                .bind()
        }
    }

    fun sendTilAvbrytelse(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val utbetaling = queries.utbetaling.getAndAcquireLock(id)
            ensureErSaksbehandler(navIdent, utbetaling)

            utbetalingService.sendTilAvbrytelse(
                id = id,
                agent = navIdent,
                aarsaker = request.aarsaker.map { it.name },
                begrunnelse = request.begrunnelse,
            ).mapLeft(UtbetalingError::Valideringsfeil)
                .bind()
        }
    }

    fun godkjennAvbrytelse(id: UUID, navIdent: NavIdent): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val utbetaling = queries.utbetaling.getAndAcquireLock(id)
            val ansatt = queries.ansatt.getOrError(navIdent)
            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforing.id)
            ensureErAttestantUtbetaling(
                ansatt,
                OkonomiBeslutningContext(
                    gjennomforingType = utbetaling.gjennomforing.type,
                    kostnadssteder = nonEmptySetOf(gjennomforing.ansvarligEnhet.enhetsnummer),
                ),
            )

            utbetalingService.godkjennAvbrytelse(id, navIdent).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    fun avslaAvbrytelse(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val utbetaling = queries.utbetaling.getAndAcquireLock(id)
            ensureErSaksbehandler(navIdent, utbetaling)

            utbetalingService.avslaAvbrytelse(
                id = id,
                besluttetAv = navIdent,
                aarsaker = request.aarsaker.map { it.name },
                begrunnelse = request.begrunnelse,
            ).mapLeft(UtbetalingError::Valideringsfeil)
                .bind()
        }
    }

    fun godkjennUtbetalingLinje(
        id: UUID,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val linje = queries.utbetalingLinje.getOrError(id)
            val utbetaling = queries.utbetaling.getAndAcquireLock(linje.utbetalingId)
            val tilsagn = queries.tilsagn.getAndAcquireLock(linje.tilsagnId)

            ensureErAttestantUtbetaling(navIdent, utbetaling, tilsagn)

            utbetalingService.godkjennUtbetalingLinje(id, navIdent).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        begrunnelse: String?,
        navIdent: NavIdent,
    ): Either<UtbetalingError, Utbetaling> = db.transaction {
        either {
            val linje = queries.utbetalingLinje.getOrError(id)
            val utbetaling = queries.utbetaling.getAndAcquireLock(linje.utbetalingId)
            val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId)

            val saksbehandlerUtbetaling = either { ensureErSaksbehandler(navIdent, utbetaling) }
            val attestantUtbetaling = either { ensureErAttestantUtbetaling(navIdent, utbetaling, tilsagn) }

            if (saksbehandlerUtbetaling.isLeft() && attestantUtbetaling.isLeft()) {
                val manglendeRoller = buildSet {
                    saksbehandlerUtbetaling.onLeft { error ->
                        if (error is UtbetalingError.ManglerTilgang) {
                            addAll(error.roller)
                        }
                    }
                    attestantUtbetaling.onLeft { error ->
                        if (error is UtbetalingError.ManglerTilgang) {
                            addAll(error.roller)
                        }
                    }
                }
                raise(
                    UtbetalingError.ManglerTilgang(
                        "Du kan ikke returnere utbetalingen fordi du mangler tilgang",
                        manglendeRoller,
                    ),
                )
            }

            utbetalingService.returnerUtbetalingLinje(
                id = id,
                aarsaker = aarsaker,
                begrunnelse = begrunnelse,
                agent = navIdent,
            ).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    fun slettUtbetaling(id: UUID, navIdent: NavIdent): Either<UtbetalingError, Unit> = db.transaction {
        either {
            val utbetaling = queries.utbetaling.getAndAcquireLock(id)
            ensureErSaksbehandler(navIdent, utbetaling)

            utbetalingService.slettUtbetaling(id).mapLeft(UtbetalingError::Valideringsfeil).bind()
        }
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = db.transaction {
        utbetalingService.oppdaterFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)
    }

    suspend fun hentVedleggForUtbetaling(
        utbetalingId: UUID,
        accessType: AccessType,
    ): Either<UtbetalingError, List<UtbetalingVedleggDto>> {
        val journalpostId = hentJournalpostId(utbetalingId).getOrElse { return it.left() }
            ?: return emptyList<UtbetalingVedleggDto>().right()

        return hentVedleggInfo(journalpostId, accessType).right()
    }

    suspend fun hentVedleggPdf(
        utbetalingId: UUID,
        dokumentInfoId: String,
        accessType: AccessType,
    ): Either<UtbetalingError, ByteArray> = either {
        val journalpostId = hentJournalpostId(utbetalingId).bind()
        ensureNotNull(journalpostId) {
            UtbetalingError.NotFound("Utbetalingen har ingen journalpost")
        }
        val vedlegg = ensureNotNull(
            hentVedleggInfo(journalpostId, accessType)
                .find { it.dokumentInfoId == dokumentInfoId },
        ) {
            UtbetalingError.NotFound("Fant ikke vedlegget med dokumentInfoId=$dokumentInfoId")
        }

        safClient.hentDokument(
            journalpostId.value,
            vedlegg.dokumentInfoId,
            vedlegg.variantFormat,
            accessType,
        )
            .mapLeft { it.toUtbetalingError() }.bind()
    }

    private fun hentJournalpostId(utbetalingId: UUID): Either<UtbetalingError, JournalpostId?> = db.session {
        either {
            val utbetaling = ensureNotNull(queries.utbetaling.get(utbetalingId)) {
                UtbetalingError.NotFound("Fant ikke utbetalingen")
            }

            utbetaling.journalpostId
        }
    }

    private suspend fun hentVedleggInfo(
        journalpostId: JournalpostId,
        accessType: AccessType,
    ): List<UtbetalingVedleggDto> {
        val journalpost = safClient.hentDokumenterForJournalpost(journalpostId.value, accessType).getOrElse {
            return emptyList()
        }

        return journalpost.dokumenter.mapNotNull { dokument ->
            val variant = dokument.tilgjengeligPdfVariant() ?: return@mapNotNull null
            UtbetalingVedleggDto(
                dokumentInfoId = dokument.dokumentInfoId,
                tittel = dokument.tittel,
                variantFormat = variant.variantformat,
            )
        }
    }

    private fun SafDokument.tilgjengeligPdfVariant(): SafDokumentvariant? = dokumentvarianter
        .filter { it.filtype == "PDF" && it.saksbehandlerHarTilgang }
        .let { varianter ->
            varianter.find { it.variantformat == "SLADDET" }
                ?: varianter.find { it.variantformat == "ARKIV" }
        }

    private fun SafError.toUtbetalingError(): UtbetalingError = when (this) {
        SafError.NotFound -> UtbetalingError.NotFound("Fant ikke journalposten eller dokumentet i SAF")
        SafError.Forbidden -> UtbetalingError.ManglerTilgang("Du mangler tilgang til journalposten i SAF", emptySet())
        SafError.Error -> UtbetalingError.SafFeil
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
    ): Either<UtbetalingError, Unit> = either {
        val type = when (gjennomforing) {
            is GjennomforingAvtale -> GjennomforingType.AVTALE
            is GjennomforingEnkeltplass -> GjennomforingType.ENKELTPLASS
        }
        val ansatt = queries.ansatt.getOrError(navIdent)
        ensureErSaksbehandler(ansatt, type)
    }

    context(errors: Raise<UtbetalingError>)
    private fun QueryContext.ensureErSaksbehandler(navIdent: NavIdent, utbetaling: Utbetaling) {
        val ansatt = queries.ansatt.getOrError(navIdent)
        return ensureErSaksbehandler(ansatt, utbetaling.gjennomforing.type)
    }

    context(errors: Raise<UtbetalingError>)
    private fun ensureErSaksbehandler(ansatt: NavAnsatt, gjennomforingType: GjennomforingType) {
        OkonomiAuthorization.erSaksbehandler(ansatt, gjennomforingType).onLeft {
            errors.raise(it.toUtbetalingError())
        }
    }

    context(errors: Raise<UtbetalingError>)
    private fun QueryContext.ensureErAttestantUtbetaling(
        navIdent: NavIdent,
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
    ) {
        val ansatt = queries.ansatt.getOrError(navIdent)
        val kontekst = OkonomiBeslutningContext(
            gjennomforingType = utbetaling.gjennomforing.type,
            kostnadssteder = nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
        )
        ensureErAttestantUtbetaling(ansatt, kontekst)
    }

    context(errors: Raise<UtbetalingError>)
    private fun ensureErAttestantUtbetaling(
        ansatt: NavAnsatt,
        kontekst: OkonomiBeslutningContext,
    ) {
        OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst).onLeft {
            errors.raise(it.toUtbetalingError())
        }
    }

    private fun OkonomiAuthorizationError.toUtbetalingError(): UtbetalingError = when (this) {
        is OkonomiAuthorizationError.ManglerTilgang -> UtbetalingError.ManglerTilgang("Du mangler tilgang til utbetalingen", requiredRoles)
        is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> UtbetalingError.ManglerTilgang("Du mangler kontorspesifikk tilgang til utbetalingen", requiredRoles)
    }
}
