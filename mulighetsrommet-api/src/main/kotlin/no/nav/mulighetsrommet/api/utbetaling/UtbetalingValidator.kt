package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangorflate.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.relevantForDeltakelse
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
import java.util.*

object UtbetalingValidator {
    fun validate(belop: Int, tilsagn: TilsagnDto, maxBelop: Int): Either<List<FieldError>, Unit> = either {
        val errors = buildList {
            if (belop <= 0) {
                add(
                    FieldError.of(
                        DelutbetalingRequest::belop,
                        "Beløp må være positivt",
                    ),
                )
            }
            // TODO: Bruk gjenstående beløp
            if (belop > tilsagn.beregning.output.belop) {
                add(
                    FieldError.of(
                        DelutbetalingRequest::belop,
                        "Beløp er større enn gjenstående på tilsagnet",
                    ),
                )
            }
            if (belop > maxBelop) {
                add(
                    FieldError.of(
                        DelutbetalingRequest::belop,
                        "Kan ikke betale ut mer enn det er krav på",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: Unit.right()
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
        utbetaling: UtbetalingDto,
        forslagByDeltakerId: Map<UUID, List<DeltakerForslag>>,
    ): Either<List<FieldError>, GodkjennUtbetaling> {
        if (utbetaling.status != UtbetalingStatus.KLAR_FOR_GODKJENNING) {
            return listOf(
                FieldError.root(
                    "Utbetaling allerede godkjent",
                ),
            ).left()
        }
        val finnesRelevanteForslag = forslagByDeltakerId
            .any { (_, forslag) ->
                forslag.count { it.relevantForDeltakelse(utbetaling) } > 0
            }

        return if (finnesRelevanteForslag) {
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
