package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient,
) {

    private val cache: Cache<String, VirksomhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10000)
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("enhetsregisterCache", cache)
    }

    suspend fun hentVirksomhet(virksomhetsnummer: String): VirksomhetDto? {
        return CacheUtils.tryCacheFirstNullable(cache, virksomhetsnummer) {
            amtEnhetsregisterClient.hentVirksomhet(virksomhetsnummer)
        }
    }

    suspend fun hentOverordnetEnhetNavnForArrangor(virksomhetsnummer: String): String? {
        val virksomhet = hentVirksomhet(virksomhetsnummer)
        val overordnetVirksomhet = virksomhet?.overordnetEnhet?.let { hentVirksomhet(it) }
        return overordnetVirksomhet?.navn
    }
}
