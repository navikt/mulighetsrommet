package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateManuellUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.RelevanteForslag
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
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
        val previous: Delutbetaling?,
    )

    fun validateOpprettDelutbetalinger(
        utbetaling: Utbetaling,
        opprettDelutbetalinger: List<OpprettDelutbetaling>,
    ): Either<List<FieldError>, List<OpprettDelutbetaling>> = either {
        val errors = buildList {
            val totalBelopUtbetales = opprettDelutbetalinger.sumOf { it.belop }
            if (totalBelopUtbetales > utbetaling.beregning.output.belop) {
                add(
                    FieldError.root(
                        "Kan ikke utbetale mer enn totalbeløpet",
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
                when (req.previous?.status) {
                    null, DelutbetalingStatus.RETURNERT -> {}
                    DelutbetalingStatus.TIL_GODKJENNING,
                    DelutbetalingStatus.GODKJENT,
                    DelutbetalingStatus.UTBETALT,
                    DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
                    DelutbetalingStatus.BEHANDLES_AV_NAV,
                    -> {
                        add(
                            FieldError.ofPointer(
                                "/$index",
                                "Utbetaling kan ikke endres fordi den har status: ${req.previous.status}",
                            ),
                        )
                    }
                }
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
                            "Beløp er større enn tilgjengelig beløp på tilsagn",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.ANNULLERT) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er annullert og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.TIL_ANNULLERING) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er til annullering og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.OPPGJORT) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er oppgjort og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.TIL_OPPGJOR) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er til oppgjør og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.TIL_OPPGJOR) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er til oppgjør og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
                if (req.tilsagn.status == TilsagnStatus.TIL_GODKJENNING) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagnet er til godkjenning og kan ikke benyttes, linjen må fjernes",
                        ),
                    )
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: opprettDelutbetalinger.right()
    }

    fun validateManuellUtbetalingskrav(
        id: UUID,
        request: OpprettManuellUtbetalingRequest,
    ): Either<List<FieldError>, ValidatedManuellUtbetalingRequest> {
        val errors = buildList {
            if (request.periodeSlutt.isBefore(request.periodeStart)) {
                add(
                    FieldError.of(
                        OpprettManuellUtbetalingRequest::periodeSlutt,
                        "Periodeslutt må være etter periodestart",
                    ),
                )
            }

            if (request.belop < 1) {
                add(FieldError.of(OpprettManuellUtbetalingRequest::belop, "Beløp må være positivt"))
            }

            if (request.beskrivelse.length < 10) {
                add(FieldError.of(OpprettManuellUtbetalingRequest::beskrivelse, "Du må fylle ut beskrivelse"))
            }

            if (request.kontonummer.value.length != 11) {
                add(FieldError.of(OpprettManuellUtbetalingRequest::kontonummer, "Kontonummer må være 11 tegn"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: ValidatedManuellUtbetalingRequest(
            id = id,
            gjennomforingId = request.gjennomforingId,
            periodeStart = request.periodeStart,
            periodeSlutt = request.periodeSlutt,
            belop = request.belop,
            kontonummer = request.kontonummer,
            kidNummer = request.kidNummer,
            beskrivelse = request.beskrivelse,
            vedlegg = emptyList(),
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        ).right()
    }

    data class ValidatedManuellUtbetalingRequest(
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

    fun validateArrangorflateManuellUtbetalingskrav(
        request: ArrangorflateManuellUtbetalingRequest,
    ): Either<List<FieldError>, ValidatedManuellUtbetalingRequest> {
        var validated: ValidatedManuellUtbetalingRequest? = null
        val errors = buildList {
            val start = try {
                LocalDate.parse(request.periodeStart)
            } catch (t: DateTimeParseException) {
                add(
                    FieldError.of(
                        ArrangorflateManuellUtbetalingRequest::periodeStart,
                        "Dato må være på formatet 'yyyy-mm-dd'",
                    ),
                )
                null
            }
            val slutt = try {
                LocalDate.parse(request.periodeSlutt)
            } catch (t: DateTimeParseException) {
                add(
                    FieldError.of(
                        ArrangorflateManuellUtbetalingRequest::periodeSlutt,
                        "Dato må være på formatet 'yyyy-mm-dd'",
                    ),
                )
                null
            }

            if (slutt != null && start != null && slutt.isBefore(start)) {
                add(
                    FieldError.of(
                        ArrangorflateManuellUtbetalingRequest::periodeStart,
                        "Periodeslutt må være etter periodestart",
                    ),
                )
            }

            if (request.belop < 1) {
                add(FieldError.of(ArrangorflateManuellUtbetalingRequest::belop, "Beløp må være positivt"))
            }

            if (request.beskrivelse.length < 10) {
                add(FieldError.of(ArrangorflateManuellUtbetalingRequest::beskrivelse, "Du må beskrive utbetalingen"))
            }

            if (request.vedlegg.isEmpty()) {
                add(FieldError.of(ArrangorflateManuellUtbetalingRequest::vedlegg, "Du må legge ved vedlegg"))
            }

            val kontonummer = try {
                Kontonummer(request.kontonummer)
            } catch (e: IllegalArgumentException) {
                add(
                    FieldError.of(
                        ArrangorflateManuellUtbetalingRequest::kontonummer,
                        "Ugyldig kontonummer",
                    ),
                )
                null
            }
            val kid: Kid? = request.kidNummer?.let {
                try {
                    Kid(it)
                } catch (e: IllegalArgumentException) {
                    add(
                        FieldError.of(
                            ArrangorflateManuellUtbetalingRequest::kontonummer,
                            "Ugyldig kid",
                        ),
                    )
                    null
                }
            }
            if (start != null && slutt != null && kontonummer != null) {
                validated = ValidatedManuellUtbetalingRequest(
                    id = UUID.randomUUID(),
                    gjennomforingId = request.gjennomforingId,
                    periodeStart = start,
                    periodeSlutt = slutt,
                    belop = request.belop,
                    beskrivelse = request.beskrivelse,
                    kontonummer = kontonummer,
                    kidNummer = kid,
                    tilskuddstype = request.tilskuddstype,
                    vedlegg = request.vedlegg,
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: validated!!.right()
    }

    fun validerGodkjennUtbetaling(
        request: GodkjennUtbetaling,
        utbetaling: Utbetaling,
        relevanteForslag: List<RelevanteForslag>,
    ): Either<List<FieldError>, GodkjennUtbetaling> {
        return if (utbetaling.innsender != null) {
            listOf(
                FieldError.root("Utbetalingen er allerede godkjent"),
            ).left()
        } else if (relevanteForslag.any { it.antallRelevanteForslag > 0 }) {
            listOf(
                FieldError.ofPointer(
                    "/info",
                    "Det finnes forslag på deltakere som påvirker utbetalingen. Disse må behandles av Nav før utbetalingen kan sendes inn.",
                ),
            ).left()
        } else if (request.digest != utbetaling.beregning.getDigest()) {
            listOf(
                FieldError.ofPointer(
                    "/info",
                    "Informasjonen i kravet har endret seg. Vennligst se over på nytt.",
                ),
            ).left()
        } else if (utbetaling.betalingsinformasjon.kontonummer == null) {
            listOf(
                FieldError.ofPointer(
                    "/info",
                    "Utbetalingen kan ikke godkjennes fordi kontonummer mangler.",
                ),
            ).left()
        } else {
            request.right()
        }
    }
}
