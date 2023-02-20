package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient
) {

    val cache: Cache<String, VirksomhetDTO> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10000)
        .build()

    suspend fun hentVirksomhet(virksomhetsnummer: String): VirksomhetDTO? {
        return CacheUtils
            .tryCacheFirstNullable(cache, virksomhetsnummer) {
                val virksomhet: VirksomhetDTO? = virksomhetsnummer.let { amtEnhetsregisterClient.hentVirksomhet(it.toInt()) }
                virksomhet
            }
    }

    suspend fun hentOverordnetEnhetNavnForArrangor(virksomhetsnummer: String): String {
        return hentVirksomhet(virksomhetsnummer)?.overordnetEnhetNavn ?: ""
    }
}
