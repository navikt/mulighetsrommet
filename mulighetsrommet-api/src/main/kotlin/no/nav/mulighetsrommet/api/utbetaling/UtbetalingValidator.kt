package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto

object UtbetalingValidator {
    fun validate(
        request: BehandleUtbetalingRequest,
        utbetalinger: List<DelutbetalingDto>,
    ): Either<List<FieldError>, BehandleUtbetalingRequest> = either {
        if (utbetalinger.isNotEmpty()) {
            return FieldError
                .of(BehandleUtbetalingRequest::kostnadsfordeling, "Utbetaling allerede opprettet.")
                .nel()
                .left()
        }

        val errors = buildList {
            request.kostnadsfordeling.forEachIndexed { index, tilsagnOgBelop ->
                if (tilsagnOgBelop.belop <= 0) {
                    add(
                        FieldError.ofPointer(
                            "/kostnadsfordeling/$index/belop",
                            "Beløp må være positivt",
                        ),
                    )
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
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
}
