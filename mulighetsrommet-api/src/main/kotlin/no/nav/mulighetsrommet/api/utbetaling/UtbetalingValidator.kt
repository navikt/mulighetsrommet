package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.*
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravOmUtbetalingRequest
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

object UtbetalingValidator {
    data class OpprettDelutbetaling(
        val id: UUID,
        val belop: Int,
        val gjorOppTilsagn: Boolean,
        val tilsagn: Tilsagn,
    )

    fun validateOpprettDelutbetalinger(
        utbetaling: Utbetaling,
        opprettDelutbetalinger: List<OpprettDelutbetaling>,
        begrunnelse: String?,
    ): Either<List<FieldError>, List<OpprettDelutbetaling>> = either {
        val errors = buildList {
            when (utbetaling.status) {
                UtbetalingStatusType.INNSENDT,
                UtbetalingStatusType.RETURNERT,
                -> Unit

                UtbetalingStatusType.GENERERT,
                UtbetalingStatusType.TIL_ATTESTERING,
                UtbetalingStatusType.FERDIG_BEHANDLET,
                ->
                    add(
                        FieldError.root(
                            "Utbetaling kan ikke endres fordi den har status: ${utbetaling.status}",
                        ),
                    )
            }
            val totalBelopUtbetales = opprettDelutbetalinger.sumOf { it.belop }
            if (totalBelopUtbetales > utbetaling.beregning.output.belop) {
                add(
                    FieldError.root(
                        "Kan ikke utbetale mer enn innsendt beløp",
                    ),
                )
            }
            if (totalBelopUtbetales < utbetaling.beregning.output.belop && begrunnelse == null) {
                add(
                    FieldError.root(
                        "Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp",
                    ),
                )
            }
            if (opprettDelutbetalinger.isEmpty()) {
                add(
                    FieldError.root(
                        "Utbetalingslinjer mangler",
                    ),
                )
            }

            opprettDelutbetalinger.forEachIndexed { index, req ->
                if (req.belop <= 0) {
                    add(
                        FieldError.ofPointer(
                            "/$index/belop",
                            "Beløp må være positivt",
                        ),
                    )
                }
                if (req.belop > req.tilsagn.gjenstaendeBelop()) {
                    add(
                        FieldError.ofPointer(
                            "/$index/belop",
                            "Kan ikke utbetale mer enn gjenstående beløp på tilsagn",
                        ),
                    )
                }
                if (req.tilsagn.status != TilsagnStatus.GODKJENT) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet har status ${req.tilsagn.status.beskrivelse} og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: opprettDelutbetalinger.right()
    }

    fun validateOpprettUtbetalingRequest(
        id: UUID,
        request: OpprettUtbetalingRequest,
    ): Either<NonEmptyList<FieldError>, OpprettUtbetaling> = either {
        zipOrAccumulate(
            {
                ensureNotNull(request.periodeStart) {
                    FieldError.of("Periodestart må være satt", OpprettUtbetalingRequest::periodeStart)
                }
                request.periodeStart
            },
            {
                ensureNotNull(request.periodeSlutt) {
                    FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt)
                }
                request.periodeSlutt
            },
            {
                ensure(request.belop > 1) {
                    FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::belop)
                }
                request.belop
            },
            {
                ensure(request.beskrivelse.length > 10) {
                    FieldError.of("Du må fylle ut beskrivelse", OpprettUtbetalingRequest::beskrivelse)
                }
                request.beskrivelse
            },
            {
                ensure(request.kontonummer.value.length == 11) {
                    FieldError.of("Kontonummer må være 11 tegn", OpprettUtbetalingRequest::kontonummer)
                }
                request.kontonummer
            },
            {
                request.kidNummer?.let { raw ->
                    ensureNotNull(Kid.parse(raw)) {
                        FieldError.of("Ugyldig kid", OpprettUtbetalingRequest::kidNummer)
                    }
                }
            },
        ) { start: LocalDate, slutt: LocalDate, belop, beskrivelse, kontonummer, kid ->
            ensure(start.isBefore(slutt)) {
                FieldError.of("Periodeslutt må være etter periodestart", OpprettUtbetalingRequest::periodeSlutt).nel()
            }
            val periode = Periode.fromInclusiveDates(start, slutt)

            OpprettUtbetaling(
                id = id,
                gjennomforingId = request.gjennomforingId,
                periodeStart = periode.start,
                periodeSlutt = periode.getLastInclusiveDate(),
                belop = belop,
                kontonummer = kontonummer,
                kidNummer = kid,
                beskrivelse = beskrivelse,
                vedlegg = emptyList(),
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            )
        }
    }

    data class OpprettUtbetaling(
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

    fun validateOpprettKravOmUtbetaling(
        request: OpprettKravOmUtbetalingRequest,
        kontonummer: Kontonummer,
    ): Either<List<FieldError>, OpprettUtbetaling> {
        val errors = buildList {
            val start = try {
                LocalDate.parse(request.periodeStart)
            } catch (t: DateTimeParseException) {
                add(
                    FieldError.of(
                        "Dato må være på formatet 'yyyy-mm-dd'",
                        OpprettKravOmUtbetalingRequest::periodeStart,
                    ),
                )
                null
            }
            val slutt = try {
                LocalDate.parse(request.periodeSlutt)
            } catch (t: DateTimeParseException) {
                add(
                    FieldError.of(
                        "Dato må være på formatet 'yyyy-mm-dd'",
                        OpprettKravOmUtbetalingRequest::periodeSlutt,
                    ),
                )
                null
            }

            if (slutt != null && start != null && slutt.isBefore(start)) {
                add(
                    FieldError.of(
                        "Periodeslutt må være etter periodestart",
                        OpprettKravOmUtbetalingRequest::periodeStart,
                    ),
                )
            }

            if (request.belop < 1) {
                add(FieldError.of("Beløp må være positivt", OpprettKravOmUtbetalingRequest::belop))
            }

            if (request.vedlegg.isEmpty()) {
                add(FieldError.of("Du må legge ved vedlegg", OpprettKravOmUtbetalingRequest::vedlegg))
            }

            if (request.kidNummer != null && Kid.parse(request.kidNummer) == null) {
                add(
                    FieldError.of(
                        "Ugyldig kid",
                        OpprettKravOmUtbetalingRequest::kidNummer,
                    ),
                )
            }
        }
        return errors.takeIf { it.isNotEmpty() }?.left()
            ?: OpprettUtbetaling(
                id = UUID.randomUUID(),
                gjennomforingId = request.gjennomforingId,
                periodeStart = LocalDate.parse(request.periodeStart),
                periodeSlutt = LocalDate.parse(request.periodeSlutt),
                belop = request.belop,
                kontonummer = kontonummer,
                kidNummer = request.kidNummer?.let { Kid.parseOrThrow(it) },
                tilskuddstype = request.tilskuddstype,
                beskrivelse = "",
                vedlegg = request.vedlegg,
            ).right()
    }

    fun validerGodkjennUtbetaling(
        request: GodkjennUtbetaling,
        utbetaling: Utbetaling,
        advarsler: List<DeltakerAdvarsel>,
        today: LocalDate,
    ): Either<List<FieldError>, Kid?> {
        val errors = buildList {
            if (utbetaling.innsender != null) {
                add(FieldError.root("Utbetalingen er allerede godkjent"))
            }
            if (!utbetaling.periode.slutt.isBefore(today)) {
                add(FieldError.root("Utbetalingen kan ikke godkjennes før perioden er passert"))
            }
            if (advarsler.isNotEmpty()) {
                add(
                    FieldError.ofPointer(
                        "/info",
                        "Det finnes advarsler på deltakere som påvirker utbetalingen. Disse må fikses før utbetalingen kan sendes inn.",
                    ),
                )
            }
            if (request.digest != utbetaling.beregning.getDigest()) {
                add(
                    FieldError.ofPointer(
                        "/info",
                        "Informasjonen i kravet har endret seg. Vennligst se over på nytt.",
                    ),
                )
            }
            if (utbetaling.betalingsinformasjon.kontonummer == null) {
                add(
                    FieldError.ofPointer(
                        "/info",
                        "Utbetalingen kan ikke godkjennes fordi kontonummer mangler.",
                    ),
                )
            }
            if (request.kid != null && Kid.parse(request.kid) == null) {
                add(
                    FieldError.of(
                        "Ugyldig kid",
                        GodkjennUtbetaling::kid,
                    ),
                )
            }
        }
        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.kid?.let { Kid.parseOrThrow(it) }.right()
    }
}
