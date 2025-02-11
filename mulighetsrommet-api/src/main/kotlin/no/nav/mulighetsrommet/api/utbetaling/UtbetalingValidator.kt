package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto

object UtbetalingValidator {
    fun validate(belop: Int, tilsagn: TilsagnDto): Either<List<FieldError>, Unit> = either {
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
}

data class TilsagnOgBelop(
    val tilsagn: TilsagnDto,
    val belop: Int,
)
