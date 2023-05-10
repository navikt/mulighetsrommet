package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class BrregService(private val brregClient: BrregClient) {

    private val brregServiceCache: Cache<String, VirksomhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("brregServiceCache", brregServiceCache)
    }

    suspend fun hentEnhet(orgnr: String): VirksomhetDto {
        return CacheUtils.tryCacheFirstNotNull(brregServiceCache, orgnr) {
            brregClient.hentEnhet(orgnr)
        }
    }

    suspend fun sokEtterEnhet(sokestreng: String): List<VirksomhetDto> {
        return brregClient.sokEtterOverordnetEnheter(sokestreng)
    }
}
