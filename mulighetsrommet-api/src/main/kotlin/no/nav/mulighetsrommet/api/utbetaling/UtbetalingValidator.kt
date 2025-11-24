package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object UtbetalingValidator {
    data class OpprettDelutbetaling(
        val id: UUID,
        val belop: Int?,
        val gjorOppTilsagn: Boolean,
        val tilsagn: Tilsagn,
    ) {
        data class Tilsagn(
            val status: TilsagnStatus,
            val gjenstaendeBelop: Int,
        )
    }

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
                -> false
            },
        ) {
            FieldError.root(
                "Utbetaling kan ikke endres fordi den har status: ${utbetaling.status}",
            )
        }
        val totalBelopUtbetales = opprettDelutbetalinger.sumOf { it.belop ?: 0 }
        validate(totalBelopUtbetales <= utbetaling.beregning.output.belop) {
            FieldError.root(
                "Kan ikke utbetale mer enn innsendt beløp",
            )
        }
        validate(totalBelopUtbetales >= utbetaling.beregning.output.belop || !begrunnelse.isNullOrBlank()) {
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
            validate(req.belop != null && req.belop > 0) {
                FieldError.ofPointer(
                    "/$index/belop",
                    "Beløp må være positivt",
                )
            }
            validate(req.belop == null || req.belop <= req.tilsagn.gjenstaendeBelop) {
                FieldError.ofPointer(
                    "/$index/belop",
                    "Kan ikke utbetale mer enn gjenstående beløp på tilsagn",
                )
            }
            validate(req.tilsagn.status == TilsagnStatus.GODKJENT) {
                FieldError.ofPointer(
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
    ): Either<List<FieldError>, OpprettAnnenAvtaltPrisUtbetaling> = validation {
        validateNotNull(request.periodeStart) {
            FieldError.of("Periodestart må være satt", OpprettUtbetalingRequest::periodeStart)
        }
        validateNotNull(request.periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt)
        }
        validate(request.belop > 1) {
            FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::belop)
        }
        validate(request.beskrivelse.length > 10) {
            FieldError.of("Du må fylle ut beskrivelse", OpprettUtbetalingRequest::beskrivelse)
        }
        validate(request.kontonummer.value.length == 11) {
            FieldError.of("Kontonummer må være 11 tegn", OpprettUtbetalingRequest::kontonummer)
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

        OpprettAnnenAvtaltPrisUtbetaling(
            id = id,
            gjennomforingId = request.gjennomforingId,
            periodeStart = periode.start,
            periodeSlutt = periode.getLastInclusiveDate(),
            belop = request.belop,
            kontonummer = request.kontonummer,
            kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
            beskrivelse = request.beskrivelse,
            vedlegg = emptyList(),
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        )
    }

    data class OpprettAnnenAvtaltPrisUtbetaling(
        val id: UUID,
        val gjennomforingId: UUID,
        val periodeStart: LocalDate,
        val periodeSlutt: LocalDate,
        val beskrivelse: String,
        val kontonummer: Kontonummer,
        val kidNummer: Kid? = null,
        val belop: Int,
        val tilskuddstype: Tilskuddstype,
        val vedlegg: List<Vedlegg>,
    )

    fun minAntallVedleggVedOpprettKrav(prismodellType: PrismodellType?): Int = when (prismodellType) {
        PrismodellType.ANNEN_AVTALT_PRIS -> 0
        PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        PrismodellType.AVTALT_PRIS_PER_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        null,
        ->
            1
    }

    fun maksUtbetalingsPeriodeSluttDato(
        prismodell: PrismodellType,
        opprettKravPeriode: Map<PrismodellType, Periode>,
        relativeDate: LocalDate = LocalDate.now(),
    ): LocalDate {
        val opprettKravPeriodeSlutt = opprettKravPeriode[prismodell]
            ?: invalidPrismodellOpprettKrav(prismodell)

        return when (prismodell) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK ->
                minOf(relativeDate, opprettKravPeriodeSlutt.slutt)

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER ->
                minOf(relativeDate.withDayOfMonth(1), opprettKravPeriodeSlutt.slutt)

            PrismodellType.ANNEN_AVTALT_PRIS ->
                opprettKravPeriodeSlutt.slutt

            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            ->
                invalidPrismodellOpprettKrav(prismodell)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun invalidPrismodellOpprettKrav(prismodell: PrismodellType): Nothing = throw IllegalArgumentException("Kan ikke opprette utbetalingskrav for prismodell: ${prismodell.navn}")

    fun validateOpprettKravArrangorflate(
        request: OpprettKravUtbetalingRequest,
        prismodellType: PrismodellType,
        opprettKravPeriode: Map<PrismodellType, Periode>,
        kontonummer: Kontonummer,
    ): Either<List<FieldError>, ValidertUtbetalingKrav> = validation {
        val start = try {
            LocalDate.parse(request.periodeStart)
        } catch (_: DateTimeParseException) {
            null
        }
        validateNotNull(start) {
            FieldError.of(
                "Dato må være på formatet 'yyyy-mm-dd'",
                OpprettKravUtbetalingRequest::periodeStart,
            )
        }
        val slutt = try {
            LocalDate.parse(request.periodeSlutt)
        } catch (_: DateTimeParseException) {
            null
        }
        validateNotNull(slutt) {
            FieldError.of(
                "Dato må være på formatet 'yyyy-mm-dd'",
                OpprettKravUtbetalingRequest::periodeSlutt,
            )
        }
        requireValid(start != null && slutt != null)

        validate(start.isBefore(slutt)) {
            FieldError.of(
                "Periodeslutt må være etter periodestart",
                OpprettKravUtbetalingRequest::periodeStart,
            )
        }

        validate(!slutt.isAfter(maksUtbetalingsPeriodeSluttDato(prismodellType, opprettKravPeriode))) {
            FieldError.of(
                "Du kan ikke sende inn for valgt periode før perioden er passert",
                OpprettKravUtbetalingRequest::periodeSlutt,
            )
        }

        validate(request.belop > 0) {
            FieldError.of("Beløp må være positivt", OpprettKravUtbetalingRequest::belop)
        }
        validate(request.vedlegg.size >= minAntallVedleggVedOpprettKrav(prismodellType)) {
            FieldError.of("Du må legge ved vedlegg", OpprettKravUtbetalingRequest::vedlegg)
        }
        requireValid(request.kidNummer == null || Kid.parse(request.kidNummer) != null) {
            FieldError.of(
                "Ugyldig kid",
                OpprettKravUtbetalingRequest::kidNummer,
            )
        }

        ValidertUtbetalingKrav(
            periodeStart = LocalDate.parse(request.periodeStart),
            periodeSlutt = LocalDate.parse(request.periodeSlutt),
            belop = request.belop,
            kontonummer = kontonummer,
            kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
            vedlegg = request.vedlegg,
        )
    }

    data class ValidertUtbetalingKrav(
        val periodeStart: LocalDate,
        val periodeSlutt: LocalDate,
        val kontonummer: Kontonummer,
        val kidNummer: Kid? = null,
        val belop: Int,
        val vedlegg: List<Vedlegg>,
    )

    fun ValidertUtbetalingKrav.toAnnenAvtaltPris(
        gjennomforingId: UUID,
        tilskuddstype: Tilskuddstype,
    ): OpprettAnnenAvtaltPrisUtbetaling {
        return OpprettAnnenAvtaltPrisUtbetaling(
            id = UUID.randomUUID(),
            gjennomforingId = gjennomforingId,
            tilskuddstype = tilskuddstype,
            periodeStart = this.periodeStart,
            periodeSlutt = this.periodeSlutt,
            beskrivelse = "",
            kontonummer = this.kontonummer,
            kidNummer = this.kidNummer,
            belop = this.belop,
            vedlegg = this.vedlegg,
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
            FieldError.ofPointer(
                "/info",
                "Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.",
            )
        }
        validate(request.digest == utbetaling.beregning.getDigest()) {
            FieldError.ofPointer("/info", "Informasjonen i kravet har endret seg. Vennligst se over på nytt.")
        }
        validate(utbetaling.betalingsinformasjon.kontonummer != null) {
            FieldError.ofPointer("/info", "Utbetalingen kan ikke godkjennes fordi kontonummer mangler.")
        }
        requireValid(request.kid == null || Kid.parse(request.kid) != null) {
            FieldError.of("Ugyldig kid", GodkjennUtbetaling::kid)
        }
        request.kid?.let { Kid.parseOrThrow(it) }
    }
}
