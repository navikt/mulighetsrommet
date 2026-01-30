package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import no.nav.mulighetsrommet.api.arrangorflate.api.AvbrytUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorAvbrytStatus
import no.nav.mulighetsrommet.api.arrangorflate.service.arrangorAvbrytStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingAnnenAvtaltPris
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.compareTo
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object UtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1

    fun validateOpprettDelutbetalinger(
        utbetaling: Utbetaling,
        opprettDelutbetalinger: List<OpprettDelutbetaling>,
        begrunnelse: String?,
    ): Either<List<FieldError>, List<OpprettDelutbetaling>> = validation {
        validate(
            when (utbetaling.status) {
                UtbetalingStatusType.INNSENDT,
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
                "Utbetaling kan ikke endres fordi den har status: ${utbetaling.status}",
            )
        }
        val totalBelopUtbetales = opprettDelutbetalinger.sumOf { it.pris?.belop ?: 0 }.withValuta(utbetaling.valuta)
        validate(totalBelopUtbetales <= utbetaling.beregning.output.pris) {
            FieldError.root(
                "Kan ikke utbetale mer enn innsendt beløp",
            )
        }
        validate(totalBelopUtbetales >= utbetaling.beregning.output.pris || !begrunnelse.isNullOrBlank()) {
            FieldError.root(
                "Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp",
            )
        }
        validate(opprettDelutbetalinger.isNotEmpty()) {
            FieldError.root(
                "Utbetalingslinjer mangler",
            )
        }

        opprettDelutbetalinger.forEachIndexed { index, req ->
            validate(req.pris != null && req.pris.belop > 0) {
                FieldError(
                    "/$index/pris",
                    "Beløp må være positivt",
                )
            }
            validate(req.pris == null || req.pris <= req.tilsagn.gjenstaendeBelop) {
                FieldError(
                    "/$index/pris",
                    "Kan ikke utbetale mer enn gjenstående beløp på tilsagn",
                )
            }
            validate(req.tilsagn.status == TilsagnStatus.GODKJENT) {
                FieldError(
                    "/$index/tilsagnId",
                    "Tilsagnet har status ${req.tilsagn.status.beskrivelse} og kan ikke benyttes, linjen må fjernes",
                )
            }
        }
        opprettDelutbetalinger
    }

    fun validateOpprettUtbetalingRequest(
        id: UUID,
        request: OpprettUtbetalingRequest,
    ): Either<List<FieldError>, OpprettUtbetalingAnnenAvtaltPris> = validation {
        validateNotNull(request.periodeStart) {
            FieldError.of("Periodestart må være satt", OpprettUtbetalingRequest::periodeStart)
        }
        validateNotNull(request.periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt)
        }
        validate(request.pris.belop > 1) {
            FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::pris)
        }
        validate(request.beskrivelse.length > 10) {
            FieldError.of("Du må fylle ut beskrivelse", OpprettUtbetalingRequest::beskrivelse)
        }
        validate(request.kidNummer == null || Kid.parse(request.kidNummer) != null) {
            FieldError.of("Ugyldig kid", OpprettUtbetalingRequest::kidNummer)
        }
        requireValid(request.periodeSlutt != null && request.periodeStart != null)
        validate(request.periodeStart.isBefore(request.periodeSlutt)) {
            FieldError.of("Periodeslutt må være etter periodestart", OpprettUtbetalingRequest::periodeSlutt)
        }
        requireValid(request.periodeStart.isBefore(request.periodeSlutt))
        requireValid(request.kidNummer == null || Kid.parse(request.kidNummer) != null)
        val periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

        OpprettUtbetalingAnnenAvtaltPris(
            id = id,
            gjennomforingId = request.gjennomforingId,
            periodeStart = periode.start,
            periodeSlutt = periode.getLastInclusiveDate(),
            pris = request.pris,
            kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
            beskrivelse = request.beskrivelse,
            vedlegg = emptyList(),
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        )
    }

    fun validerGodkjennUtbetaling(
        request: GodkjennUtbetaling,
        utbetaling: Utbetaling,
        advarsler: List<DeltakerAdvarsel>,
        today: LocalDate,
    ): Either<List<FieldError>, Kid?> = validation {
        validate(utbetaling.innsender == null) {
            FieldError.root("Utbetalingen er allerede godkjent")
        }
        validate(utbetaling.periode.slutt.isBefore(today)) {
            FieldError.root("Utbetalingen kan ikke godkjennes før perioden er passert")
        }
        validate(advarsler.isEmpty()) {
            FieldError(
                "/info",
                "Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.",
            )
        }
        validate(request.digest == utbetaling.beregning.getDigest()) {
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
            FieldError.of("Begrunnelse ikke være lengre enn 100 tegn", AvbrytUtbetaling::begrunnelse)
        }
        request.begrunnelse
    }

    fun validerRegenererUtbetaling(utbetaling: Utbetaling): Either<List<FieldError>, Unit> = validation {
        validate(utbetaling.status == UtbetalingStatusType.AVBRUTT) {
            FieldError.root("Utbetalingen kan ikke regenereres")
        }
        validate(utbetaling.innsender == Arrangor) {
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
