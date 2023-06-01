package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.ktor.exception.StatusException
import java.time.LocalDate
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val virksomhetService: VirksomhetService,
) {
    fun get(id: UUID): QueryResult<AvtaleAdminDto?> {
        return avtaler.get(id)
    }

    suspend fun upsert(avtale: AvtaleRequest): QueryResult<AvtaleAdminDto> {
        val avtaleDbo = avtale.toDbo()

        virksomhetService.hentEnhet(avtale.leverandorOrganisasjonsnummer)
            ?: throw BadRequestException("leverandør ${avtale.leverandorOrganisasjonsnummer} finnes ikke")

        return avtaler.upsert(avtaleDbo)
            .flatMap { avtaler.get(avtaleDbo.id) }
            .map { it!! } // If upsert is succesfull it should exist here
    }

    fun delete(id: UUID, currentDate: LocalDate = LocalDate.now()): SletteAvtaleDto {
        val optionalAvtale = avtaler.get(id).getOrNull()
            ?: throw NotFoundException("Fant ikke avtale for sletting")

        if (optionalAvtale.opphav == ArenaMigrering.Opphav.ARENA) {
            return SletteAvtaleDto(
                statusCode = HttpStatusCode.BadRequest.value,
                message = "Avtalen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate.",
            )
        }

        if (optionalAvtale.startDato <= currentDate && optionalAvtale.sluttDato >= currentDate) {
            return SletteAvtaleDto(
                statusCode = HttpStatusCode.BadRequest.value,
                message = "Avtalen er mellom start- og sluttdato og må avsluttes før den kan slettes.",
            )
        }

        val gjennomforingerForAvtale =
            tiltaksgjennomforinger.getAll(
                filter = AdminTiltaksgjennomforingFilter(
                    avtaleId = id,
                    dagensDato = currentDate,
                ),
            )
                .getOrElse { Pair(0, emptyList()) }
        if (gjennomforingerForAvtale.first > 0) {
            return SletteAvtaleDto(
                statusCode = HttpStatusCode.BadRequest.value,
                message = "Avtalen har ${gjennomforingerForAvtale.first} ${if (gjennomforingerForAvtale.first > 1) "tiltaksgjennomføringer" else "tiltaksgjennomføring"} koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen.",
            )
        }

        val deleteResponse = avtaler.delete(id)

        return deleteResponse.map {
            SletteAvtaleDto(statusCode = HttpStatusCode.OK.value, message = "Avtalen ble slettet")
        }.mapLeft {
            SletteAvtaleDto(
                statusCode = HttpStatusCode.InternalServerError.value,
                "Det oppsto en feil ved sletting av avtalen",
                cause = it.error.message,
            )
        }.getOrNull() ?: throw StatusException(
            status = HttpStatusCode.InternalServerError,
            description = "Det skjedde en feil ved sletting av avtale med id: $id",
        )
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

@Serializable
data class SletteAvtaleDto(
    val statusCode: Int,
    val message: String,
    val cause: String? = null,
)
