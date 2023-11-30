package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.repositories.VeilederJoyrideRepository

class VeilederJoyrideService(val veilederJoyrideRepository: VeilederJoyrideRepository) {
    fun save(data: VeilederJoyrideDto) {
        if (data.navident.length != 7) {
            throw Exception("navident må være 7 tegn")
        }

        veilederJoyrideRepository.save(data)
    }

    fun harFullfortJoyride(navident: String, type: JoyrideType): Boolean {
        return veilederJoyrideRepository.harFullfortJoyride(navident, type)
    }
}
