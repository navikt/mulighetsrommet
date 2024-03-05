package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftGraphService(private val client: MicrosoftGraphClient) {

    private val ansattDataCache: Cache<UUID, AzureAdNavAnsatt> = Caffeine.newBuilder()
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
        val cacheMetrics: CacheMetricsCollector = CacheMetricsCollector()
            .register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("ansattDataCache", ansattDataCache)
        cacheMetrics.addCache("brukerAzureIdToAdGruppeCache", navAnsattAdGrupperCache)
    }

    suspend fun getNavAnsatt(navAnsattAzureId: UUID, accessType: AccessType): AzureAdNavAnsatt {
        return CacheUtils.tryCacheFirstNotNull(ansattDataCache, navAnsattAzureId) {
            client.getNavAnsatt(navAnsattAzureId, accessType)
        }
    }

    suspend fun getNavAnsattAdGrupper(navAnsattAzureId: UUID, accessType: AccessType): List<AdGruppe> {
        return CacheUtils.tryCacheFirstNotNull(navAnsattAdGrupperCache, navAnsattAzureId) {
            client.getMemberGroups(navAnsattAzureId, accessType)
        }
    }

    suspend fun getNavAnsatteInGroup(groupId: UUID): List<AzureAdNavAnsatt> {
        return client.getGroupMembers(groupId)
    }
}
