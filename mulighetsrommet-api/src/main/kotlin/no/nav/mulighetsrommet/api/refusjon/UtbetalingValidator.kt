package no.nav.mulighetsrommet.api.refusjon

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.refusjon.model.TilsagnUtbetalingDto
import no.nav.mulighetsrommet.api.responses.FieldError

object UtbetalingValidator {
    fun validate(
        request: UtbetalingRequest,
        utbetalinger: List<TilsagnUtbetalingDto>,
    ): Either<List<FieldError>, UtbetalingRequest> = either {
        if (utbetalinger.isNotEmpty()) {
            return FieldError
                .of(UtbetalingRequest::kostnadsfordeling, "Utbetaling allerede opprettet.")
                .nel()
                .left()
        }

        val errors = buildList {
            if (request.kostnadsfordeling.any { it.belop <= 0 }) {
                add(
                    FieldError.of(
                        UtbetalingRequest::kostnadsfordeling,
                        "Beløp må være positivt",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
    }
}
