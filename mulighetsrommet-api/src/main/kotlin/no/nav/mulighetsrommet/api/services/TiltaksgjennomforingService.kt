package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import no.nav.mulighetsrommet.api.repositories.AnsattTiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto

class TiltaksgjennomforingService(private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository, private val ansattTiltaksgjennomforingRepository: AnsattTiltaksgjennomforingRepository) {

    fun getAll(paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingDto>> {
        return tiltaksgjennomforingRepository.getAll(paginationParams)
    }

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingDto> {
        return tiltaksgjennomforingRepository.sok(filter)
    }

    fun getAllByEnhet(enhet: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingDto>> {
        return tiltaksgjennomforingRepository.getAllByEnhet(enhet, paginationParams)
    }

    fun getAllForAnsatt(navIdent: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingDto>> {
        return tiltaksgjennomforingRepository.getAllByNavident(navIdent, paginationParams)
    }

    fun lagreGjennomforingTilMinliste(tiltaksgjennomforingId: String, navIdent: String): String {
        val id = ansattTiltaksgjennomforingRepository.lagreFavoritt(tiltaksgjennomforingId, navIdent).getOrElse { "" }
        println(id)
        return id
    }

    fun fjernGjennomforingFraMinliste(tiltaksgjennomforingId: String, navIdent: String) {
        ansattTiltaksgjennomforingRepository.fjernFavoritt(tiltaksgjennomforingId, navIdent)
    }
}

data class Sokefilter(
    val tiltaksnummer: String
)
