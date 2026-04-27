package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.api.AvbrytUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorAvbrytStatus
import no.nav.mulighetsrommet.api.arrangorflate.service.arrangorAvbrytStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object UtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1

    data class OpprettUtbetalingLinjerCtx(
        val utbetaling: Utbetaling,
        val linjer: List<Linje>,
        val begrunnelse: String?,
    ) {
        data class Linje(
            val request: UtbetalingLinjeRequest,
            val tilsagn: Tilsagn,
        )

        data class Tilsagn(
            val status: TilsagnStatus,
            val gjenstaendeBelop: ValutaBelop,
        )
    }

    data class ValidatedLinje(
        val id: UUID,
        val pris: ValutaBelop,
        val gjorOppTilsagn: Boolean,
    )

    fun validateOpprettUtbetalingLinjer(
        ctx: OpprettUtbetalingLinjerCtx,
    ): Either<List<FieldError>, List<ValidatedLinje>> = validation {
        validate(
            when (ctx.utbetaling.status) {
                UtbetalingStatusType.TIL_BEHANDLING,
                UtbetalingStatusType.RETURNERT,
                -> true

                UtbetalingStatusType.GENERERT,
                UtbetalingStatusType.TIL_ATTESTERING,
                UtbetalingStatusType.FERDIG_BEHANDLET,
                UtbetalingStatusType.DELVIS_UTBETALT,
                UtbetalingStatusType.UTBETALT,
                UtbetalingStatusType.AVBRUTT,
                -> false
            },
        ) {
            FieldError.root(
                "Utbetaling kan ikke endres fordi den har status: ${ctx.utbetaling.status}",
            )
        }
        val totalBelopUtbetales = ctx.linjer.sumOf { it.request.pris?.belop ?: 0 }.withValuta(ctx.utbetaling.valuta)
        validate(totalBelopUtbetales <= ctx.utbetaling.beregning.output.pris) {
            FieldError.root(
                "Kan ikke utbetale mer enn innsendt beløp",
            )
        }
        validate(totalBelopUtbetales >= ctx.utbetaling.beregning.output.pris || !ctx.begrunnelse.isNullOrBlank()) {
            FieldError.root(
                "Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp",
            )
        }
        validate(ctx.linjer.isNotEmpty()) {
            FieldError.root(
                "Utbetalingslinjer mangler",
            )
        }

        ctx.linjer.mapIndexed { index, linje ->
            requireValid(linje.request.pris?.belop != null && linje.request.pris.belop > 0 && linje.request.pris.valuta != null) {
                FieldError(
                    "/utbetalingLinjer/$index/pris/belop",
                    "Beløp må være positivt",
                )
            }
            val pris = ValutaBelop(linje.request.pris.belop, linje.request.pris.valuta)
            validate(pris <= linje.tilsagn.gjenstaendeBelop) {
                FieldError(
                    "/utbetalingLinjer/$index/tilsagnId",
                    "Beløp overstiger gjenstående beløp på tilsagn. For å utbetale hele beløpet må dere først opprette og godkjenne et ekstratilsagn",
                )
            }
            validate(linje.tilsagn.status == TilsagnStatus.GODKJENT) {
                FieldError(
                    "/utbetalingLinjer/$index/tilsagnId",
                    "Tilsagnet har status ${linje.tilsagn.status.beskrivelse} og kan ikke benyttes, linjen må fjernes",
                )
            }
            ValidatedLinje(
                id = linje.request.id,
                pris = pris,
                gjorOppTilsagn = linje.request.gjorOppTilsagn ?: false,
            )
        }
    }

    fun validateUpsertUtbetaling(
        request: UtbetalingRequest,
        arrangor: ArrangorDto? = null,
    ): Either<List<FieldError>, UpsertUtbetaling> = validation {
        validateNotNull(request.periodeStart) {
            FieldError.of("Periodestart må være satt", UtbetalingRequest::periodeStart)
        }
        validateNotNull(request.periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", UtbetalingRequest::periodeSlutt)
        }
        validate(request.pris?.belop != null && request.pris.belop >= 1) {
            FieldError.of("Beløp må være positivt", UtbetalingRequest::pris, ValutaBelopRequest::belop)
        }

        val kid = request.kidNummer?.let { value ->
            validateNotNull(Kid.parse(value)) {
                FieldError.of("Ugyldig kid", UtbetalingRequest::kidNummer)
            }
        }

        val utenlandskArrangor = arrangor?.erUtenlandsk ?: false
        val journalpostId = when (request.korrigererUtbetaling) {
            null -> {
                if (utenlandskArrangor) {
                    null
                } else {
                    validateNotNull(request.journalpostId?.let { JournalpostId.parse(it) }) {
                        FieldError.of("Journalpost-ID er på ugyldig format", UtbetalingRequest::journalpostId)
                    }
                }
            }

            else -> null
        }

        val kommentar = request.kommentar?.trim()?.takeIf { it.isNotEmpty() }?.also { value ->
            validate(value.length >= 10) {
                FieldError.of("Kommentar må være minst 10 tegn", UtbetalingRequest::kommentar)
            }

            validate(value.length <= 250) {
                FieldError.of("Kommentar kan ikke være mer enn 250 tegn", UtbetalingRequest::kommentar)
            }
        }

        val korreksjonBegrunnelse = when (request.korrigererUtbetaling) {
            null -> null

            else -> request.korreksjonBegrunnelse?.trim().also { value ->
                val length = value.orEmpty().length
                validate(length >= 10) {
                    FieldError.of(
                        "Begrunnelse for korreksjon må være minst 10 tegn",
                        UtbetalingRequest::korreksjonBegrunnelse,
                    )
                }

                validate(length <= 250) {
                    FieldError.of(
                        "Begrunnelse for korreksjon kan ikke være mer enn 250 tegn",
                        UtbetalingRequest::korreksjonBegrunnelse,
                    )
                }
            }
        }

        requireValid(requireNotNull(request.periodeStart) <= requireNotNull(request.periodeSlutt)) {
            FieldError.of("Periodeslutt må være etter periodestart", UtbetalingRequest::periodeSlutt)
        }
        val periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

        val beregning = requireNotNull(request.pris).let { (belop, valuta) ->
            UtbetalingBeregningFri.from(ValutaBelop(requireNotNull(belop), requireNotNull(valuta)))
        }

        if (request.korrigererUtbetaling != null) {
            UpsertUtbetaling.Korreksjon(
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                id = request.id,
                periode = periode,
                beregning = beregning,
                kid = kid,
                korreksjonGjelderUtbetalingId = request.korrigererUtbetaling,
                korreksjonBegrunnelse = requireNotNull(korreksjonBegrunnelse),
                kommentar = kommentar,
            )
        } else {
            if (!utenlandskArrangor) {
                requireNotNull(journalpostId)
            }
            UpsertUtbetaling.Anskaffelse(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periode = periode,
                journalpostId = journalpostId,
                beregning = beregning,
                kid = kid,
                kommentar = kommentar,
                vedlegg = emptyList(),
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            )
        }
    }

    fun validerGodkjennUtbetaling(
        request: GodkjennUtbetaling,
        utbetaling: Utbetaling,
        advarsler: List<DeltakerAdvarsel>,
        today: LocalDate,
    ): Either<List<FieldError>, Kid?> = validation {
        when (utbetaling.status) {
            UtbetalingStatusType.GENERERT -> Unit

            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            -> error { FieldError.root("Utbetalingen er allerede godkjent") }
        }
        validate(utbetaling.blokkeringer.isEmpty() && advarsler.isEmpty()) {
            FieldError(
                "/info",
                "Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.",
            )
        }
        validate(utbetaling.periode.slutt.isBefore(today)) {
            FieldError.root("Utbetalingen kan ikke godkjennes før perioden er passert")
        }
        validate(request.updatedAt == utbetaling.updatedAt.toString()) {
            FieldError("/info", "Informasjonen i kravet har endret seg. Vennligst se over på nytt.")
        }
        validate(utbetaling.betalingsinformasjon != null) {
            FieldError("/info", "Utbetalingen kan ikke godkjennes fordi kontonummer mangler.")
        }
        requireValid(request.kid == null || Kid.parse(request.kid) != null) {
            FieldError.of("Ugyldig kid", GodkjennUtbetaling::kid)
        }
        request.kid?.let { Kid.parseOrThrow(it) }
    }

    fun validerAvbrytUtbetaling(
        request: AvbrytUtbetaling,
        utbetaling: Utbetaling,
    ): Either<List<FieldError>, String> = validation {
        validate(arrangorAvbrytStatus(utbetaling) == ArrangorAvbrytStatus.ACTIVATED) {
            FieldError.root("Utbetalingen kan ikke avbrytes")
        }
        requireValid(!request.begrunnelse.isNullOrBlank()) {
            FieldError.of("Begrunnelse må være satt", AvbrytUtbetaling::begrunnelse)
        }
        validate(request.begrunnelse.length <= 100) {
            FieldError.of("Begrunnelse kan ikke være lengre enn 100 tegn", AvbrytUtbetaling::begrunnelse)
        }
        request.begrunnelse
    }

    // TODO: inline i GenererUtbetalingService
    fun validerRegenererUtbetaling(utbetaling: Utbetaling): Either<List<FieldError>, Unit> = validation {
        validate(utbetaling.status == UtbetalingStatusType.AVBRUTT) {
            FieldError.root("Utbetalingen kan ikke regenereres")
        }
        validateNotNull(utbetaling.innsending) {
            FieldError.root("Utbetalingen kan ikke regenereres")
        }
        when (utbetaling.beregning) {
            is UtbetalingBeregningFri,
            is UtbetalingBeregningPrisPerTimeOppfolging,
            -> error {
                FieldError.root("Utbetalingen kan ikke regenereres")
            }

            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerUkesverk,
            -> Unit
        }
    }
}
