package no.nav.mulighetsrommet.api.veilederflate.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.veilederflate.models.NavEnhetDto
import no.nav.mulighetsrommet.api.veilederflate.models.toDto
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.concurrent.TimeUnit

class NavEnhetService(
    private val navEnhetRepository: NavEnhetRepository,
) {
    private val cache: Cache<NavEnhetNummer, NavEnhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    fun hentEnhet(enhetsnummer: NavEnhetNummer): NavEnhetDto? {
        cache.getIfPresent(enhetsnummer)?.let { return it }

        return navEnhetRepository.get(enhetsnummer)?.toDto()
            ?.also { cache.put(enhetsnummer, it) }
    }

    fun hentOverordnetFylkesenhet(enhetsnummer: NavEnhetNummer): NavEnhetDto? {
        var enhet = hentEnhet(enhetsnummer) ?: return null

        while (enhet.type != NavEnhetType.FYLKE) {
            val overordnetEnhet = enhet.overordnetEnhet ?: break
            enhet = hentEnhet(overordnetEnhet) ?: return null
        }

        return enhet
    }
}
