package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.AvtaleNotificationDto
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

    suspend fun upsert(avtale: AvtaleDbo): StatusResponse<AvtaleAdminDto> {
        virksomhetService.hentEnhet(avtale.leverandorOrganisasjonsnummer)
            ?: return Either.Left(BadRequest(message = "leverandør ${avtale.leverandorOrganisasjonsnummer} finnes ikke"))

        return avtaler.upsert(avtale)
            .flatMap { avtaler.get(avtale.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .mapLeft { ServerError("Internal Error while upserting avtale: $it") }
    }

    fun delete(id: UUID, currentDate: LocalDate = LocalDate.now()): StatusResponse<Unit> {
        val optionalAvtale = avtaler.get(id).getOrNull()
            ?: return Either.Left(NotFound("Fant ikke avtale for sletting"))

        if (optionalAvtale.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Avtalen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate."))
        }

        if (optionalAvtale.startDato <= currentDate) {
            return Either.Left(BadRequest(message = "Avtalen er aktiv og kan derfor ikke slettes."))
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
            return Either.Left(BadRequest(message = "Avtalen har ${gjennomforingerForAvtale.first} ${if (gjennomforingerForAvtale.first > 1) "tiltaksgjennomføringer" else "tiltaksgjennomføring"} koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen."))
        }

        return avtaler
            .delete(id)
            .map {}
            .mapLeft {
                ServerError(message = "Det oppsto en feil ved sletting av avtalen")
            }
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
