package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val deltakerRepository: DeltakerRepository
) {

    suspend fun getAll(paginationParams: PaginationParams, filter: AdminTiltaksgjennomforingFilter): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        val (totalCount, items) = tiltaksgjennomforingRepository.getAll(paginationParams, filter)

        val avtalerMedLeverandorNavn = items.hentVirksomhetsnavnForTiltaksgjennomforinger()

        return totalCount to avtalerMedLeverandorNavn
    }

    private suspend fun List<TiltaksgjennomforingAdminDto>.hentVirksomhetsnavnForTiltaksgjennomforinger(): List<TiltaksgjennomforingAdminDto> {
        return this.map {
            it.hentVirksomhetsnavnForTiltaksgjennomforing()
        }
    }

    private suspend fun TiltaksgjennomforingAdminDto.hentVirksomhetsnavnForTiltaksgjennomforing(): TiltaksgjennomforingAdminDto {
        val virksomhet = this.virksomhetsnummer?.let { arrangorService.hentVirksomhet(it) }
        if (virksomhet != null) {
            return this.copy(virksomhetsnavn = virksomhet.navn)
        }
        return this
    }

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingAdminDto> {
        return tiltaksgjennomforingRepository.sok(filter)
    }

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto {
        val antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId)
        return TiltaksgjennomforingNokkeltallDto(antallDeltakere = antallDeltakere)
    }
}

data class Sokefilter(
    val tiltaksnummer: String
)
