package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorAvbrytStatus
import no.nav.mulighetsrommet.api.arrangorflate.api.AvbrytUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.arrangorAvbrytStatus
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.compareTo
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object UtbetalingValidator {
    const val MIN_ANTALL_VEDLEGG_OPPRETT_KRAV = 1
    data class OpprettDelutbetaling(
        val id: UUID,
        val pris: ValutaBelop?,
        val gjorOppTilsagn: Boolean,
        val tilsagn: Tilsagn,
    ) {
        data class Tilsagn(
            val status: TilsagnStatus,
            val gjenstaendeBelop: ValutaBelop,
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
    ): Either<List<FieldError>, OpprettAnnenAvtaltPrisUtbetaling> = validation {
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

        OpprettAnnenAvtaltPrisUtbetaling(
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

    data class OpprettAnnenAvtaltPrisUtbetaling(
        val id: UUID,
        val gjennomforingId: UUID,
        val periodeStart: LocalDate,
        val periodeSlutt: LocalDate,
        val beskrivelse: String,
        val kidNummer: Kid? = null,
        val pris: ValutaBelop,
        val tilskuddstype: Tilskuddstype,
        val vedlegg: List<Vedlegg>,
    )

    fun maksUtbetalingsPeriodeSluttDato(
        gjennomforing: GjennomforingGruppetiltak,
        okonomiConfig: OkonomiConfig,
        relativeDate: LocalDate = LocalDate.now(),
    ): LocalDate {
        val opprettKravPeriodeSluttDato =
            okonomiConfig.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode]?.slutt
                ?: invalidGjennomforingOpprettKrav(gjennomforing)

        return when (gjennomforing.prismodell?.type) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> minOf(relativeDate, opprettKravPeriodeSluttDato)

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> minOf(
                relativeDate.withDayOfMonth(1),
                opprettKravPeriodeSluttDato,
            )

            PrismodellType.ANNEN_AVTALT_PRIS -> opprettKravPeriodeSluttDato

            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            null,
            -> invalidGjennomforingOpprettKrav(gjennomforing)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun invalidGjennomforingOpprettKrav(gjennomforing: GjennomforingGruppetiltak): Nothing = throw IllegalArgumentException("Kan ikke opprette utbetalingskrav for ${gjennomforing.tiltakstype.navn} med prismodell ${gjennomforing.prismodell?.type?.navn}")

    fun validateOpprettKravArrangorflate(
        request: OpprettKravUtbetalingRequest,
        gjennomforing: GjennomforingGruppetiltak,
        okonomiConfig: OkonomiConfig,
        kontonummer: Kontonummer,
    ): Either<List<FieldError>, ValidertUtbetalingKrav> = validation {
        requireNotNull(gjennomforing.prismodell)

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

        validate(!slutt.isAfter(maksUtbetalingsPeriodeSluttDato(gjennomforing, okonomiConfig))) {
            FieldError.of(
                "Du kan ikke sende inn for valgt periode før perioden er passert",
                OpprettKravUtbetalingRequest::periodeSlutt,
            )
        }

        validate(request.belop > 0) {
            FieldError.of("Beløp må være positivt", OpprettKravUtbetalingRequest::belop)
        }
        validate(request.vedlegg.size >= MIN_ANTALL_VEDLEGG_OPPRETT_KRAV) {
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
            pris = request.belop.withValuta(gjennomforing.prismodell.valuta),
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
        val pris: ValutaBelop,
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
            kidNummer = this.kidNummer,
            pris = this.pris,
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
