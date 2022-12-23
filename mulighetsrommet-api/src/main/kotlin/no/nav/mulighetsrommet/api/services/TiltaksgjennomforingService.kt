package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.models.TiltaksgjennomforingMedTiltakstype

class TiltaksgjennomforingService(private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository) {

    fun getAll(paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingMedTiltakstype>> {
        return tiltaksgjennomforingRepository.getAll(paginationParams)
    }

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingMedTiltakstype> {
        return tiltaksgjennomforingRepository.sok(filter)
    }

    fun getAllByEnhet(enhet: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingMedTiltakstype>> {
        return tiltaksgjennomforingRepository.getAllByEnhet(enhet, paginationParams)
    }
}

data class Sokefilter(
    val tiltaksnummer: String
)
