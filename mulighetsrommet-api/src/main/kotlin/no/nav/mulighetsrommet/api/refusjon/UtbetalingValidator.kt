package no.nav.mulighetsrommet.api.refusjon

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.refusjon.model.TilsagnUtbetalingDto
import no.nav.mulighetsrommet.api.responses.ValidationError

object UtbetalingValidator {
    fun validate(
        request: UtbetalingRequest,
        utbetalinger: List<TilsagnUtbetalingDto>,
    ): Either<List<ValidationError>, UtbetalingRequest> = either {
        if (utbetalinger.isNotEmpty()) {
            return ValidationError
                .of(UtbetalingRequest::kostnadsfordeling, "Utbetaling allerede opprettet.")
                .nel()
                .left()
        }

        val errors = buildList {
            if (request.kostnadsfordeling.any { it.belop <= 0 }) {
                add(
                    ValidationError.of(
                        UtbetalingRequest::kostnadsfordeling,
                        "Beløp må være positivt",
                    ),
                )
            }


        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
    }

    fun validateManuellUtbetalingskrav(
        request: OpprettManuellUtbetalingkravRequest
    ): Either<List<ValidationError>, OpprettManuellUtbetalingkravRequest> {

        val errors = buildList {

            if (request.periode.slutt.isBefore(request.periode.start)) {
                add(
                    ValidationError.ofCustomLocation(
                        "arrangorinfo.periode.slutt",
                        "Periodeslutt må være etter periodestart"
                    )
                )
            }

            if (request.belop < 1) {
                add(ValidationError.ofCustomLocation("arrangorinfo.belop", "Beløp må være positivt"))
            }

            if (request.beskrivelse.length < 10) {
                add(ValidationError.ofCustomLocation("arrangorinfo.beskrivelse", "Du må beskrive utbetalingen"))
            }

            if (request.kontonummer.value.length != 11) {
                add(ValidationError.ofCustomLocation("arrangorinfo.kontonummer", "Kontonummer må være 11 tegn"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
    }
}
