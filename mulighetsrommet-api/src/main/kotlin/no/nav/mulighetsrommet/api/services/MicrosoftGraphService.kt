package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.UUID
import java.util.concurrent.TimeUnit

class MicrosoftGraphService(private val client: MicrosoftGraphClient) {

    private val hovedenhetCache: Cache<UUID, String> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("hovedenhetCache", hovedenhetCache)
    }

    suspend fun hentHovedEnhetForNavAnsatt(navAnsattAzureId: UUID): String {
        return CacheUtils.tryCacheFirstNotNull(hovedenhetCache, navAnsattAzureId) {
            client.hentHovedenhetForBruker(navAnsattAzureId)
        }
    }
}
