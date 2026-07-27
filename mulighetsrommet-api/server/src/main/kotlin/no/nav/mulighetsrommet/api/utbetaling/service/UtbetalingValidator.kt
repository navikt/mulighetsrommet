package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.validation
import no.nav.tiltak.okonomi.Tilskuddstype
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object UtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1

    fun validateUpsertUtbetaling(
        request: UtbetalingRequest,
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

        val journalpostId = when (request.korrigererUtbetaling) {
            null -> request.journalpostId?.let { value ->
                validateNotNull(JournalpostId.parse(value)) {
                    FieldError.of("Journalpost-ID er på ugyldig format", UtbetalingRequest::journalpostId)
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
            UpsertUtbetaling.Anskaffelse(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periode = periode,
                journalpostId = journalpostId,
                beregning = beregning,
                kid = kid,
                kommentar = kommentar,
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            )
        }
    }
}
