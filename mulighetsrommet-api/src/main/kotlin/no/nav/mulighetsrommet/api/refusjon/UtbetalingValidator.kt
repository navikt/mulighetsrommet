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
        request: OpprettManuellUtbetalingkravRequest
    ): Either<List<FieldError>, OpprettManuellUtbetalingkravRequest> {

        val errors = buildList {
            if (request.periode.slutt.isBefore(request.periode.start)) {
                add(
                    FieldError.ofPointer(
                        "/arrangorinfo/periode/slutt",
                        "Periodeslutt må være etter periodestart"
                    )
                )
            }

            if (request.belop < 1) {
                add( FieldError.ofPointer("/arrangorinfo/belop", "Beløp må være positivt"))
            }

            if (request.beskrivelse.length < 10) {
                add( FieldError.ofPointer("/arrangorinfo/beskrivelse", "Du må beskrive utbetalingen"))
            }

            if (request.kontonummer.value.length != 11) {
                add( FieldError.ofPointer("/arrangorinfo/kontonummer", "Kontonummer må være 11 tegn"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
    }
}
