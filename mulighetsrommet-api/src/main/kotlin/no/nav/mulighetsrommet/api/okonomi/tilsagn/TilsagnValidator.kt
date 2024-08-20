package no.nav.mulighetsrommet.api.okonomi.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.domain.dto.NavIdent

class TilsagnValidator(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
) {
    fun validate(request: TilsagnRequest, previous: TilsagnDto?, navIdent: NavIdent): Either<List<ValidationError>, TilsagnDbo> = either {
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

        return next.right()
    }
}
