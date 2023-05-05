package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftGraphService(private val client: MicrosoftGraphClient) {

    private val ansattDataCache: Cache<UUID, NavAnsattDto> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    private val navAnsattAdGrupperCache: Cache<UUID, List<AdGruppe>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("ansattDataCache", ansattDataCache)
        cacheMetrics.addCache("brukerAzureIdToAdGruppeCache", navAnsattAdGrupperCache)
    }

    suspend fun getNavAnsatt(accessToken: String, navAnsattAzureId: UUID): NavAnsattDto {
        return CacheUtils.tryCacheFirstNotNull(ansattDataCache, navAnsattAzureId) {
            client.getNavAnsatt(accessToken, navAnsattAzureId)
        }
    }

    suspend fun getNavAnsattAdGrupper(navAnsattAzureId: UUID): List<AdGruppe> {
        return CacheUtils.tryCacheFirstNotNull(navAnsattAdGrupperCache, navAnsattAzureId) {
            client.getMemberGroups(navAnsattAzureId)
        }
    }
}
