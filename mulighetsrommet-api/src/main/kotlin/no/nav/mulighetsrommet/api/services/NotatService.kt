package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotatDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotatDto
import no.nav.mulighetsrommet.api.repositories.AvtaleNotatRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingNotatRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.Forbidden
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.utils.NotatFilter
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.slf4j.LoggerFactory
import java.util.*

class NotatService(
    private val avtaleNotatRepository: AvtaleNotatRepository,
    private val tiltaksgjennomforingNotatRepository: TiltaksgjennomforingNotatRepository,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun getAllAvtaleNotater(
        filter: NotatFilter,
    ): StatusResponse<List<AvtaleNotatDto>> {
        return avtaleNotatRepository.getAll(filter = filter)
            .mapLeft { error -> ServerError(message = "Det oppsto en feil ved henting av notater for avtale. Error: $error") }
    }

    fun upsertAvtaleNotat(notat: AvtaleNotatDbo): StatusResponse<AvtaleNotatDto> {
        logger.info("Upserter avtalenotat med id: $notat.id")
        return avtaleNotatRepository.upsert(notat)
            .flatMap { avtaleNotatRepository.get(notat.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting notat for avtale: $it") }
    }

    fun getAvtaleNotat(id: UUID): QueryResult<AvtaleNotatDto?> {
        return avtaleNotatRepository.get(id)
    }

    fun deleteAvtaleNotat(id: UUID, navIdent: NavIdent): StatusResponse<Int> {
        logger.info("Prøver å slette avtalenotat med id: '$id'")
        val notatForSletting = avtaleNotatRepository.get(id).getOrThrow()

        if (notatForSletting?.opprettetAv?.navIdent != navIdent) {
            logger.info("Kan ikke slette notat med id: '$id' som du ikke har opprettet selv.")
            return Either.Left(Forbidden(message = "Kan ikke slette notat som du ikke har opprettet selv."))
        }

        return avtaleNotatRepository
            .delete(id)
            .mapLeft {
                ServerError(message = "Det oppsto en feil ved sletting av notat for avtalen")
            }
    }

    fun getAllTiltaksgjennomforingNotater(filter: NotatFilter): StatusResponse<List<TiltaksgjennomforingNotatDto>> {
        return tiltaksgjennomforingNotatRepository.getAll(filter = filter)
            .mapLeft { error -> ServerError(message = "Det oppsto en feil ved henting av notater for tiltaksgjennomføring. Error: $error") }
    }

    fun upsertTiltaksgjennomforingNotat(notat: TiltaksgjennomforingNotatDbo): StatusResponse<TiltaksgjennomforingNotatDto> {
        logger.info("Upserter avtalenotat med id: $notat.id")
        return tiltaksgjennomforingNotatRepository.upsert(notat)
            .flatMap { tiltaksgjennomforingNotatRepository.get(notat.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting notat for tiltaksgjennomføring: $it") }
    }

    fun getTiltaksgjennomforingNotat(id: UUID): QueryResult<TiltaksgjennomforingNotatDto?> {
        return tiltaksgjennomforingNotatRepository.get(id)
    }

    fun deleteTiltaksgjennomforingNotat(id: UUID, navIdent: NavIdent): StatusResponse<Int> {
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
