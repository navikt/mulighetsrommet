package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.AvtaleNotificationDto
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val virksomhetService: VirksomhetService,
) {
    fun get(id: UUID): QueryResult<AvtaleAdminDto?> {
        return avtaler.get(id)
    }

    suspend fun upsert(avtale: AvtaleDbo): StatusResponse<AvtaleAdminDto> {
        virksomhetService.hentEnhet(avtale.leverandorOrganisasjonsnummer)
            ?: throw BadRequestException("leverandør ${avtale.leverandorOrganisasjonsnummer} finnes ikke")

        return avtaler.upsert(avtale)
            .flatMap { avtaler.get(avtale.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting avtale: $it")  }
    }

    fun delete(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams(),
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(filter, pagination)

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit,
            ),
        )
    }

    fun getNokkeltallForAvtaleMedId(id: UUID): AvtaleNokkeltallDto {
        val antallTiltaksgjennomforinger = avtaler.countTiltaksgjennomforingerForAvtaleWithId(id)
        val antallDeltakereForAvtale = tiltaksgjennomforinger.countDeltakereForAvtaleWithId(id)
        return AvtaleNokkeltallDto(
            antallTiltaksgjennomforinger = antallTiltaksgjennomforinger,
            antallDeltakere = antallDeltakereForAvtale,
        )
    }

    fun getAllAvtalerSomNarmerSegSluttdato(): List<AvtaleNotificationDto> {
        return avtaler.getAllAvtalerSomNarmerSegSluttdato()
    }
}
