package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotatDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingNotatRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.Forbidden
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.utils.TiltaksgjennomforingNotatFilter
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.getOrThrow
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingNotatService(
    private val tiltaksgjennomforingNotatRepository: TiltaksgjennomforingNotatRepository,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun getAll(
        filter: TiltaksgjennomforingNotatFilter,
    ): StatusResponse<List<TiltaksgjennomforingNotatDto>> {
        return tiltaksgjennomforingNotatRepository.getAll(filter = filter)
            .mapLeft { error -> ServerError(message = "Det oppsto en feil ved henting av notater for tiltaksgjennomføring. Error: $error") }
    }

    fun upsert(notat: TiltaksgjennomforingNotatDbo): StatusResponse<TiltaksgjennomforingNotatDto> {
        logger.info("Upserter avtalenotat med id: $notat.id")
        return tiltaksgjennomforingNotatRepository.upsert(notat)
            .flatMap { tiltaksgjennomforingNotatRepository.get(notat.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting notat for tiltaksgjennomføring: $it") }
    }

    fun get(id: UUID): QueryResult<TiltaksgjennomforingNotatDto?> {
        return tiltaksgjennomforingNotatRepository.get(id)
    }

    fun delete(id: UUID, navIdent: String): StatusResponse<Int> {
        logger.info("Prøver å slette notat for tiltaksgjennomføring med id: '$id'")
        val notatForSletting = tiltaksgjennomforingNotatRepository.get(id).getOrThrow()

        if (notatForSletting?.opprettetAv?.navIdent != navIdent) {
            logger.info("Kan ikke slette notat med id: '$id' som du ikke har opprettet selv.")
            return Either.Left(Forbidden(message = "Kan ikke slette notat som du ikke har opprettet selv."))
        }

        return tiltaksgjennomforingNotatRepository
            .delete(id)
            .mapLeft {
                ServerError(message = "Det oppsto en feil ved sletting av notat for tiltaksgjennomføringen")
            }
    }
}
