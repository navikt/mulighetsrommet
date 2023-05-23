package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class NavEnhetService(private val enhetRepository: NavEnhetRepository) {

    val cache: Cache<String, NavEnhetDbo> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("enhetCache", cache)
    }

    fun hentEnhet(enhetsnummer: String): NavEnhetDbo? {
        return CacheUtils.tryCacheFirstNullable(cache, enhetsnummer) {
            enhetRepository.get(enhetsnummer)
        }
    }

    fun hentAlleEnheter(filter: EnhetFilter): List<NavEnhetDbo> {
        return enhetRepository.getAll(filter.statuser, filter.typer)
    }

    fun hentEnheterForAvtale(filter: EnhetFilter): List<NavEnhetDbo> {
        return enhetRepository.getAllEnheterWithAvtale(filter.statuser, filter.tiltakstypeId)
    }
}
