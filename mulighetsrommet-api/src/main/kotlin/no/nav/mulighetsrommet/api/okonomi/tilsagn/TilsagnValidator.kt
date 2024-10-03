package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.domain.dto.NavIdent

class TilsagnValidator(
    val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
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

        if (previous?.besluttelse?.utfall == TilsagnBesluttelse.GODKJENT) {
            return ValidationError
                .of(TilsagnDto::id, "Tilsagnet er godkjent og kan ikke endres.")
                .nel()
                .left()
        }
        if (previous?.annullertTidspunkt != null) {
            return ValidationError
                .of(TilsagnDto::id, "Tilsagnet er annullert og kan ikke endres.")
                .nel()
                .left()
        }

        val next = request.toDbo(navIdent, gjennomforing.arrangor.id)

        val errors = buildList {
            if (next.periodeStart.year != next.periodeSlutt.year) {
                add(ValidationError.of(TilsagnDto::periodeSlutt, "Perioden kan ikke gå over flere år"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: next.right()
    }

    fun validateAFTBeregningInput(input: AFTTilsagnBeregningInput): Either<List<ValidationError>, AFTTilsagnBeregningInput> = either {
        val errors = buildList {
            if (input.periodeStart.year != input.periodeSlutt.year) {
                add(ValidationError.of(AFTTilsagnBeregningInput::periodeSlutt, "Perioden kan ikke gå over flere år"))
            }
            if (input.periodeStart.isAfter(input.periodeSlutt)) {
                add(ValidationError.of(AFTTilsagnBeregningInput::periodeSlutt, "Slutt kan ikke være før start"))
            }
            if (input.antallPlasser <= 0) {
                add(ValidationError.of(AFTTilsagnBeregningInput::antallPlasser, "Antall plasser kan ikke være 0"))
            }
            if (Prismodell.AFT.findSats(input.periodeStart) != input.sats) {
                add(ValidationError.ofCustomLocation("beregning.sats", "Feil sats for valgt periode"))
            }
            if (Prismodell.AFT.findSats(input.periodeStart) != Prismodell.AFT.findSats(input.periodeSlutt)) {
                add(ValidationError.of(AFTTilsagnBeregningInput::periodeStart, "Periode går over flere satser"))
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: input.right()
    }
}
