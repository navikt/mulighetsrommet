package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.AnsattTiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto

class TiltaksgjennomforingService(private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository, private val ansattTiltaksgjennomforingRepository: AnsattTiltaksgjennomforingRepository) {

    fun getAll(paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        return tiltaksgjennomforingRepository.getAll(paginationParams)
    }

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingAdminDto> {
        return tiltaksgjennomforingRepository.sok(filter)
    }

    fun getAllByEnhet(enhet: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        return tiltaksgjennomforingRepository.getAllByEnhet(enhet, paginationParams)
    }

    fun getAllForAnsatt(navIdent: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingDto>> {
        return tiltaksgjennomforingRepository.getAllByNavident(navIdent, paginationParams)
    }

    fun lagreGjennomforingTilMinliste(tiltaksgjennomforingId: String, navIdent: String) {
        ansattTiltaksgjennomforingRepository.lagreFavoritt(tiltaksgjennomforingId, navIdent)
    }

    fun fjernGjennomforingFraMinliste(tiltaksgjennomforingId: String, navIdent: String) {
        ansattTiltaksgjennomforingRepository.fjernFavoritt(tiltaksgjennomforingId, navIdent)
    }
}

data class Sokefilter(
    val tiltaksnummer: String
)
