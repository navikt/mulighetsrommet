package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.model.Tiltakskode

object TilsagnValidator {
    fun validate(
        next: TilsagnDbo,
        previous: TilsagnDto?,
    ): Either<List<FieldError>, TilsagnDbo> = either {
        if (previous != null && previous.status != TilsagnStatus.RETURNERT) {
            return FieldError
                .of(TilsagnDto::id, "Tilsagnet kan ikke endres.")
                .nel()
                .left()
        }

        val errors = buildList {
            if (next.periode.start.year != next.periode.getLastInclusiveDate().year) {
                add(
                    FieldError.of(
                        TilsagnRequest::periodeSlutt,
                        "Tilsagnsperioden kan ikke vare utover årsskiftet",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: next.right()
    }

    fun validateForhandsgodkjentSats(
        tiltakskode: Tiltakskode,
        input: TilsagnBeregningForhandsgodkjent.Input,
    ): Either<List<FieldError>, TilsagnBeregningForhandsgodkjent.Input> = either {
        val errors = buildList {
            val satsPeriodeStart = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periodeStart)
            if (satsPeriodeStart == null) {
                add(
                    FieldError.of(
                        "Sats mangler for valgt periode",
                        TilsagnBeregningForhandsgodkjent.Input::periodeStart,
                    ),
                )
            }

            val satsPeriodeSlutt = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periodeSlutt)
            if (satsPeriodeSlutt == null) {
                add(
                    FieldError.of(
                        "Sats mangler for valgt periode",
                        TilsagnBeregningForhandsgodkjent.Input::periodeSlutt,
                    ),
                )
            }

            if (satsPeriodeStart != satsPeriodeSlutt) {
                add(
                    FieldError.of(
                        "Periode går over flere satser",
                        TilsagnBeregningForhandsgodkjent.Input::periodeSlutt,
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }

    fun validateBeregningInput(input: TilsagnBeregningInput): Either<List<FieldError>, TilsagnBeregningInput> = either {
        return when (input) {
            is TilsagnBeregningForhandsgodkjent.Input -> validateAFTTilsagnBeregningInput(input)
            is TilsagnBeregningFri.Input -> input.right()
        }
    }

    private fun validateAFTTilsagnBeregningInput(input: TilsagnBeregningForhandsgodkjent.Input): Either<List<FieldError>, TilsagnBeregningInput> = either {
        val errors = buildList {
            if (input.periodeStart.year != input.periodeSlutt.year) {
                add(
                    FieldError.of(
                        "Tilsagnsperioden kan ikke vare utover årsskiftet",
                        TilsagnBeregningForhandsgodkjent.Input::periodeSlutt,
                    ),
                )
            }
            if (input.periodeStart.isAfter(input.periodeSlutt)) {
                add(
                    FieldError.of(
                        "Slutt kan ikke være før start",
                        TilsagnBeregningForhandsgodkjent.Input::periodeSlutt,
                    ),
                )
            }
            if (input.antallPlasser <= 0) {
                add(
                    FieldError.of(
                        "Antall plasser kan ikke være 0",
                        TilsagnBeregningForhandsgodkjent.Input::antallPlasser,
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }
}
