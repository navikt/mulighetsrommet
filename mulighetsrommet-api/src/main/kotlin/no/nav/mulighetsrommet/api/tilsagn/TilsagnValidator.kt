package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.domain.Tiltakskode

object TilsagnValidator {
    fun validate(
        next: TilsagnDbo,
        previous: TilsagnDto?,
    ): Either<List<ValidationError>, TilsagnDbo> = either {
        if (previous != null && previous.status !is TilsagnDto.TilsagnStatus.Returnert) {
            return ValidationError
                .of(TilsagnDto::id, "Tilsagnet kan ikke endres.")
                .nel()
                .left()
        }

        val errors = buildList {
            if (next.periodeStart.year != next.periodeSlutt.year) {
                add(
                    ValidationError.of(
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
    ): Either<List<ValidationError>, TilsagnBeregningForhandsgodkjent.Input> = either {
        val errors = buildList {
            val satsPeriodeStart = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periodeStart)
            if (satsPeriodeStart == null) {
                add(ValidationError.ofCustomLocation("periodeStart", "Sats mangler for valgt periode"))
            }

            val satsPeriodeSlutt = ForhandsgodkjenteSatser.findSats(tiltakskode, input.periodeSlutt)
            if (satsPeriodeSlutt == null) {
                add(ValidationError.ofCustomLocation("periodeSlutt", "Sats mangler for valgt periode"))
            }

            if (satsPeriodeStart != satsPeriodeSlutt) {
                add(ValidationError.ofCustomLocation("periodeSlutt", "Periode går over flere satser"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }

    fun validateBeregningInput(input: TilsagnBeregningInput): Either<List<ValidationError>, TilsagnBeregningInput> = either {
        return when (input) {
            is TilsagnBeregningForhandsgodkjent.Input -> validateAFTTilsagnBeregningInput(input)
            is TilsagnBeregningFri.Input -> input.right()
        }
    }

    private fun validateAFTTilsagnBeregningInput(input: TilsagnBeregningForhandsgodkjent.Input): Either<List<ValidationError>, TilsagnBeregningInput> = either {
        val errors = buildList {
            if (input.periodeStart.year != input.periodeSlutt.year) {
                add(
                    ValidationError.ofCustomLocation(
                        "periodeSlutt",
                        "Tilsagnsperioden kan ikke vare utover årsskiftet",
                    ),
                )
            }
            if (input.periodeStart.isAfter(input.periodeSlutt)) {
                add(ValidationError.ofCustomLocation("periodeSlutt", "Slutt kan ikke være før start"))
            }
            if (input.antallPlasser <= 0) {
                add(ValidationError.ofCustomLocation("beregning.antallPlasser", "Antall plasser kan ikke være 0"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }
}
