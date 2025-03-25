package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.RelevanteForslag
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
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
                        "Kan ikke betale ut mer enn det er krav på",
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
                    -> {
                        add(
                            FieldError.ofPointer(
                                "/$index",
                                "Utbetaling kan ikke endres",
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
                if (req.belop > req.tilsagn.belopGjenstaende) {
                    add(
                        FieldError.ofPointer(
                            "/$index/belop",
                            "Beløp er større enn gjenstående på tilsagnet",
                        ),
                    )
                }
                if (req.tilsagn.status != TilsagnStatus.GODKJENT) {
                    add(
                        FieldError.ofPointer(
                            "/$index/tilsagnId",
                            "Tilsagn er ikke godkjent, denne må fjernes",
                        ),
                    )
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: opprettDelutbetalinger.right()
    }

    fun validateManuellUtbetalingskrav(
        request: OpprettManuellUtbetalingRequest,
    ): Either<List<FieldError>, OpprettManuellUtbetalingRequest> {
        val errors = buildList {
            if (request.periode.slutt.isBefore(request.periode.start)) {
                add(
                    FieldError.ofPointer(
                        "/arrangorinfo/periode/slutt",
                        "Periodeslutt må være etter periodestart",
                    ),
                )
            }

            if (request.belop < 1) {
                add(FieldError.ofPointer("/arrangorinfo/belop", "Beløp må være positivt"))
            }

            if (request.beskrivelse.length < 10) {
                add(FieldError.ofPointer("/arrangorinfo/beskrivelse", "Du må beskrive utbetalingen"))
            }

            if (request.kontonummer.value.length != 11) {
                add(FieldError.ofPointer("/arrangorinfo/kontonummer", "Kontonummer må være 11 tegn"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
    }

    fun validerGodkjennUtbetaling(
        request: GodkjennUtbetaling,
        utbetaling: Utbetaling,
        relevanteForslag: List<RelevanteForslag>,
    ): Either<List<FieldError>, GodkjennUtbetaling> {
        if (utbetaling.innsender != null) {
            return listOf(
                FieldError.root(
                    "Utbetaling allerede godkjent",
                ),
            ).left()
        }
        return if (relevanteForslag.any { it.antallRelevanteForslag > 0 }) {
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
        } else {
            request.right()
        }
    }
}
