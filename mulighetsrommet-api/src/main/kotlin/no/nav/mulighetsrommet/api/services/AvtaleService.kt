package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.AvtaleNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val arrangorService: ArrangorService,
    private val navEnhetService: NavEnhetService,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
) {

    suspend fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)
            ?.hentEnhetsnavnForAvtale()
            ?.hentVirksomhetsnavnForAvtale()
    }

    fun upsert(avtale: AvtaleRequest): QueryResult<AvtaleDbo> {
        val avtaleDbo = avtale.toDbo()
        val result = avtaler.upsert(avtaleDbo)
        return result
    }

    fun delete(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    suspend fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams()
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(filter, pagination)

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
            it.hentVirksomhetsnavnForAvtale()
        }
    }

    private suspend fun AvtaleAdminDto.hentVirksomhetsnavnForAvtale(): AvtaleAdminDto {
        val virksomhet = arrangorService.hentVirksomhet(this.leverandor.organisasjonsnummer)
        return this.copy(leverandor = this.leverandor.copy(navn = virksomhet?.navn))
    }

    private fun List<AvtaleAdminDto>.hentEnhetsnavnForAvtaler(): List<AvtaleAdminDto> {
        return this.map {
            it.hentEnhetsnavnForAvtale()
        }
    }

    private fun AvtaleAdminDto.hentEnhetsnavnForAvtale(): AvtaleAdminDto {
        val enhet = navEnhetService.hentEnhet(this.navEnhet.enhetsnummer)
        return this.copy(navEnhet = this.navEnhet.copy(navn = enhet?.navn))
    }

    fun getNokkeltallForAvtaleMedId(id: UUID): AvtaleNokkeltallDto {
        val antallTiltaksgjennomforinger = avtaler.countTiltaksgjennomforingerForAvtaleWithId(id)
        val antallDeltakereForAvtale = tiltaksgjennomforinger.countDeltakereForAvtaleWithId(id)
        return AvtaleNokkeltallDto(
            antallTiltaksgjennomforinger = antallTiltaksgjennomforinger,
            antallDeltakere = antallDeltakereForAvtale
        )
    }
}
