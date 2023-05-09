package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
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
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient,
) {
    suspend fun get(id: UUID): QueryResult<AvtaleAdminDto?> {
        return avtaler.get(id)
            .map { it?.hentVirksomhetsnavnForAvtale() }
    }

    suspend fun upsert(avtale: AvtaleRequest): QueryResult<AvtaleAdminDto> {
        val avtaleDbo = avtale.toDbo()
        validerOrganisasjonsnummerForLeverandor(avtale.leverandorOrganisasjonsnummer)

        return avtaler.upsert(avtaleDbo)
            .flatMap { avtaler.get(avtaleDbo.id) }
            .map { it!! } // If upsert is succesfull it should exist here
    }

    private suspend fun validerOrganisasjonsnummerForLeverandor(leverandorOrganisasjonsnummer: String) {
        amtEnhetsregisterClient.hentVirksomhet(leverandorOrganisasjonsnummer)
            ?: throw BadRequestException("Fant ikke virksomhet med organisasjonsnummer $leverandorOrganisasjonsnummer. Kan derfor ikke lagre avtalen.")
    }

    fun delete(id: UUID): QueryResult<Int> {
        return avtaler.delete(id)
    }

    suspend fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams(),
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(filter, pagination)

        val avtalerMedLeverandorNavn = items
            .map { it.hentVirksomhetsnavnForAvtale() }

        return PaginatedResponse(
            data = avtalerMedLeverandorNavn,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit,
            ),
        )
    }

    private suspend fun AvtaleAdminDto.hentVirksomhetsnavnForAvtale(): AvtaleAdminDto {
        val virksomhet = arrangorService.hentVirksomhet(this.leverandor.organisasjonsnummer)
        return this.copy(leverandor = this.leverandor.copy(navn = virksomhet?.navn))
    }

    fun getNokkeltallForAvtaleMedId(id: UUID): AvtaleNokkeltallDto {
        val antallTiltaksgjennomforinger = avtaler.countTiltaksgjennomforingerForAvtaleWithId(id)
        val antallDeltakereForAvtale = tiltaksgjennomforinger.countDeltakereForAvtaleWithId(id)
        return AvtaleNokkeltallDto(
            antallTiltaksgjennomforinger = antallTiltaksgjennomforinger,
            antallDeltakere = antallDeltakereForAvtale,
        )
    }
}
