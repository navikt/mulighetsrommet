package no.nav.mulighetsrommet.api.services

import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val deltakerRepository: DeltakerRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> =
        tiltaksgjennomforingRepository.get(id)
            .map { it?.hentVirksomhetsnavnForTiltaksgjennomforing() }

    suspend fun getAll(
        paginationParams: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): QueryResult<Pair<Int, List<TiltaksgjennomforingAdminDto>>> =
        tiltaksgjennomforingRepository
            .getAll(paginationParams, filter)
            .map { (totalCount, items) ->
                totalCount to items.map { it.hentVirksomhetsnavnForTiltaksgjennomforing() }
            }

    suspend fun upsert(tiltaksgjennomforingDbo: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> =
        tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingDbo)
            .flatMap { tiltaksgjennomforingRepository.get(tiltaksgjennomforingDbo.id) }
            .map { it!! } // If upsert is succesfull it should exist here
            .onRight {
                sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it)
            }

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto =
        TiltaksgjennomforingNokkeltallDto(
            antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId),
        )

    private suspend fun TiltaksgjennomforingAdminDto.hentVirksomhetsnavnForTiltaksgjennomforing(): TiltaksgjennomforingAdminDto {
        val virksomhet = this.virksomhetsnummer.let { arrangorService.hentVirksomhet(it) }
        if (virksomhet != null) {
            return this.copy(virksomhetsnavn = virksomhet.navn)
        }
        return this
    }
}
