package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.okonomi.Prismodell.TilsagnBeregning
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningInput
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.NavIdent

class TilsagnValidator(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
) {
    fun validate(
        request: TilsagnRequest,
        previous: TilsagnDto?,
        navIdent: NavIdent,
    ): Either<List<ValidationError>, TilsagnDbo> = either {
        val gjennomforing = tiltaksgjennomforingRepository.get(request.tiltaksgjennomforingId)
            ?: return@validate ValidationError
                .of(TilsagnRequest::tiltaksgjennomforingId, "Tiltaksgjennomforingen finnes ikke")
                .nel()
                .left()

        if (previous != null && previous.status !is TilsagnDto.TilsagnStatus.Returnert) {
            return ValidationError
                .of(TilsagnDto::id, "Tilsagnet kan ikke endres.")
                .nel()
                .left()
        }

        when (request.beregning) {
            is TilsagnBeregning.AFT -> {
                if (gjennomforing.tiltakstype.tiltakskode != Tiltakskode.ARBEIDSFORBEREDENDE_TRENING) {
                    return ValidationError
                        .of(TilsagnDto::id, "Feil prismodell")
                        .nel()
                        .left()
                }
            }

            is TilsagnBeregning.Fri -> {
                if (gjennomforing.tiltakstype.tiltakskode == Tiltakskode.ARBEIDSFORBEREDENDE_TRENING) {
                    return ValidationError
                        .of(TilsagnDto::id, "Feil prismodell")
                        .nel()
                        .left()
                }
            }
        }

        val next = request.toDbo(navIdent, gjennomforing.arrangor.id)

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

    fun validateBeregningInput(input: TilsagnBeregningInput): Either<List<ValidationError>, TilsagnBeregningInput> = either {
        return when (input) {
            is TilsagnBeregningInput.AFT -> validateAFTTilsagnBeregningInput(input)
            is TilsagnBeregningInput.Fri -> input.right()
        }
    }

    private fun validateAFTTilsagnBeregningInput(input: TilsagnBeregningInput.AFT): Either<List<ValidationError>, TilsagnBeregningInput> = either {
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
            if (Prismodell.AFT.findSats(input.periodeStart) != Prismodell.AFT.findSats(input.periodeSlutt)) {
                add(ValidationError.ofCustomLocation("periodeSlutt", "Periode går over flere satser"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }
}
