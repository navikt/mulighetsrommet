package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.Norg2Enhet
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import no.nav.mulighetsrommet.api.utils.EnhetFilter

class EnhetService(private val enhetRepository: EnhetRepository) {
    fun hentEnheter(
        filter: EnhetFilter
    ): List<Norg2Enhet> {
        return enhetRepository.getAll(filter)
    }
}
