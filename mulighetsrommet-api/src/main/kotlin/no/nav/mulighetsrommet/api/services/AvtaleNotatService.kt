package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotatDto
import no.nav.mulighetsrommet.api.repositories.AvtaleNotatRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.Forbidden
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.utils.AvtaleNotatFilter
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.getOrThrow
import org.slf4j.LoggerFactory
import java.util.*

class AvtaleNotatService(
    private val avtaleNotatRepository: AvtaleNotatRepository,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun getAll(
        filter: AvtaleNotatFilter,
    ): StatusResponse<List<AvtaleNotatDto>> {
        return avtaleNotatRepository.getAll(filter = filter)
            .mapLeft { error -> ServerError(message = "Det oppsto en feil ved henting av notater for avtale. Error: $error") }
    }

    fun upsert(avtaleNotat: AvtaleNotatDbo): StatusResponse<AvtaleNotatDto> {
        logger.info("Upserter avtalenotat med id: $avtaleNotat.id")
        return avtaleNotatRepository.upsert(avtaleNotat)
            .flatMap { avtaleNotatRepository.get(avtaleNotat.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting notat for avtale: $it") }
    }

    fun get(id: UUID): QueryResult<AvtaleNotatDto?> {
        return avtaleNotatRepository.get(id)
    }

    fun delete(id: UUID, navIdent: String): StatusResponse<Int> {
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
}
