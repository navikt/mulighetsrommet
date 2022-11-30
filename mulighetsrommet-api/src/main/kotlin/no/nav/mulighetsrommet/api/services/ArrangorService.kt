package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.poao_tilgang.client.utils.CacheUtils
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient
) {

    val cache: Cache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10000)
        .build()

    suspend fun hentArrangornavn(virksomhetsnummer: String): String? {
        return CacheUtils
            .tryCacheFirstNotNull(cache, virksomhetsnummer) {
                val virksomhet = virksomhetsnummer.let { amtEnhetsregisterClient.hentVirksomhet(it.toInt()) }
                virksomhet?.overordnetEnhetNavn ?: ""
            }
            .ifEmpty { null }
    }
}
