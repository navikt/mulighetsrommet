package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.domain.models.TiltaksgjennomforingMedTiltakstype

class TiltaksgjennomforingService(private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository) {
    fun sok(filter: Sokefilter): List<TiltaksgjennomforingMedTiltakstype> {
        return tiltaksgjennomforingRepository.sok(filter)
    }
}

data class Sokefilter(
    val tiltaksnummer: String
)
