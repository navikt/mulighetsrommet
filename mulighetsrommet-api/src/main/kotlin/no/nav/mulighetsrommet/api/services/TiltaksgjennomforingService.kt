package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingService(
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val deltakerRepository: DeltakerRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val enhetService: NavEnhetService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun get(id: UUID): StatusResponse<TiltaksgjennomforingAdminDto> =
        tiltaksgjennomforingRepository.get(id)
            .mapLeft {
                log.error("Klarte ikke hente tiltaksgjennomføring", it.error)
                ServerError("Klarte ikke hente tiltaksgjennomføring med id=$id")
            }
            .flatMap { it?.right() ?: NotFound("Ingen tiltaksgjennomføring med id=$id").left() }
            .map { withVirksomhetsnavn(it) }

    suspend fun getAll(
        paginationParams: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): StatusResponse<PaginatedResponse<TiltaksgjennomforingAdminDto>> =
        tiltaksgjennomforingRepository
            .getAll(paginationParams, filter)
            .mapLeft {
                log.error("Klarte ikke hente tiltaksgjennomføringer", it.error)
                ServerError("Klarte ikke hente tiltaksgjennomføringer")
            }
            .map { (totalCount, items) ->
                val data = items.map { withVirksomhetsnavn(it) }
                PaginatedResponse(
                    pagination = Pagination(
                        totalCount = totalCount,
                        currentPage = paginationParams.page,
                        pageSize = paginationParams.limit,
                    ),
                    data = data,
                )
            }

    suspend fun upsert(gjennomforing: TiltaksgjennomforingRequest): StatusResponse<TiltaksgjennomforingAdminDto> {
        return Either
            .catch { tiltakstyper.get(gjennomforing.tiltakstypeId) }
            .mapLeft {
                log.error("Klarte ikke hente tiltakstype", it)
                ServerError("Klarte ikke hente tiltakstype for id=${gjennomforing.tiltakstypeId}")
            }
            .flatMap { it?.right() ?: BadRequest("Tiltakstype mangler for id=${gjennomforing.tiltakstypeId}").left() }
            .flatMap { gjennomforing.toDbo(it) }
            .flatMap { dbo ->
                virksomhetService.syncEnhetFraBrreg(dbo.virksomhetsnummer)
                tiltaksgjennomforingRepository.upsert(dbo)
                    .flatMap { tiltaksgjennomforingRepository.get(gjennomforing.id) }
                    .mapLeft {
                        log.error("Klarte ikke lagre tiltaksgjennomføring", it.error)
                        ServerError("Klarte ikke lagre tiltaksgjennomføring")
                    }
                    .flatMap { dto -> dto?.right() ?: ServerError("Klarte ikke lagre tiltaksgjennomføring").left() }
            }
            .onRight {
                sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
            }
    }

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto =
        TiltaksgjennomforingNokkeltallDto(
            antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId),
        )

    private suspend fun withVirksomhetsnavn(tiltaksgjennomforing: TiltaksgjennomforingAdminDto): TiltaksgjennomforingAdminDto {
        return arrangorService.hentVirksomhet(tiltaksgjennomforing.virksomhetsnummer)
            ?.let { tiltaksgjennomforing.copy(virksomhetsnavn = it.navn) }
            ?: tiltaksgjennomforing
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllGjennomforingerSomNarmerSegSluttdato()
    }
}
