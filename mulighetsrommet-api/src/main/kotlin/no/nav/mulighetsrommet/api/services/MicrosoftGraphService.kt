package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.msgraph.AnsattDataDTO
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftGraphService(private val client: MicrosoftGraphClient) {

    private val ansattDataCache: Cache<UUID, AnsattDataDTO> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("ansattDataCache", ansattDataCache)
    }

    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): AnsattDataDTO {
        return CacheUtils.tryCacheFirstNotNull(ansattDataCache, navAnsattAzureId) {
            client.hentAnsattdata(accessToken, navAnsattAzureId)
        }
    }
}
