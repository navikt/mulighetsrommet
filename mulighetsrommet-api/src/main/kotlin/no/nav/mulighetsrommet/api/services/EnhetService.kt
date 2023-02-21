package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.domain.Norg2Enhet
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class EnhetService(private val enhetRepository: EnhetRepository) {

    val cache: Cache<String, Norg2Enhet> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("enhetCache", cache)
    }

    fun hentEnheter(
        filter: EnhetFilter
    ): List<Norg2Enhet> {
        return enhetRepository.getAll(filter)
    }

    fun hentEnhet(enhet: String): Norg2Enhet? {
        return CacheUtils.tryCacheFirstNullable(cache, enhet) {
            enhetRepository.get(enhet)
        }
    }
}
