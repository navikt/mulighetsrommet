package no.nav.mulighetsrommet.admin.navenhet

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.concurrent.TimeUnit

data class GetNavEnhet(val enhetsnummer: NavEnhetNummer)

class NavEnhetDtoQuery(
    private val db: AdminDatabase,
) {
    private val cache: Cache<NavEnhetNummer, NavEnhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    fun execute(query: GetNavEnhet): NavEnhetDto? {
        cache.getIfPresent(query.enhetsnummer)?.let { return it }

        return db
            .session { repository.navEnhet.get(query.enhetsnummer)?.toDto() }
            ?.also { cache.put(query.enhetsnummer, it) }
    }
}
