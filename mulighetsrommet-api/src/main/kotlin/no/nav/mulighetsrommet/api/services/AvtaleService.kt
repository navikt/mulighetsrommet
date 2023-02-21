package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val arrangorService: ArrangorService,
    private val enhetService: EnhetService
) {

    fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)?.hentEnhetsnavnForAvtale()
    }

    suspend fun getAll(pagination: PaginationParams): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(pagination)

        return PaginatedResponse(
            data = items.hentVirksomhetsnavnForAvtaler().hentEnhetsnavnForAvtaler(),
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit
            )
        )
    }

    suspend fun getAvtalerForTiltakstype(
        tiltakstypeId: UUID,
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams()
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAvtalerForTiltakstype(tiltakstypeId, filter, pagination)

        val avtalerMedLeverandorNavn = items
            .hentVirksomhetsnavnForAvtaler()
            .hentEnhetsnavnForAvtaler()

        return PaginatedResponse(
            data = avtalerMedLeverandorNavn,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit
            )
        )
    }

    private suspend fun List<AvtaleAdminDto>.hentVirksomhetsnavnForAvtaler(): List<AvtaleAdminDto> {
        return this.map {
            val virksomhet = arrangorService.hentVirksomhet(it.leverandorOrganisasjonsnummer)
            it.copy(leverandornavn = virksomhet?.navn ?: null)
        }
    }

    private fun List<AvtaleAdminDto>.hentEnhetsnavnForAvtaler(): List<AvtaleAdminDto> {
        return this.map {
            val enhet = enhetService.hentEnhet(it.enhet)
            it.copy(enhetsnavn = enhet?.navn ?: null)
        }
    }

    private fun AvtaleAdminDto.hentEnhetsnavnForAvtale(): AvtaleAdminDto {
        val enhet = enhetService.hentEnhet(this.enhet)
        return this.copy(enhetsnavn = enhet?.navn)
    }
}
