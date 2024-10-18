package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftGraphService(private val client: MicrosoftGraphClient) {
    private val ansattDataCache: Cache<UUID, AzureAdNavAnsatt> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(2000)
        .recordStats()
        .build()

    private val ansattDataCacheByNavIdent: Cache<NavIdent, AzureAdNavAnsatt> = Caffeine.newBuilder()
        .expireAfterWrite(4, TimeUnit.HOURS)
        .maximumSize(2000)
        .recordStats()
        .build()

    private val navAnsattAdGrupperCache: Cache<UUID, List<AdGruppe>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun getNavAnsatt(navAnsattAzureId: UUID, accessType: AccessType): AzureAdNavAnsatt {
        return CacheUtils.tryCacheFirstNotNull(ansattDataCache, navAnsattAzureId) {
            client.getNavAnsatt(navAnsattAzureId, accessType)
        }
    }

    suspend fun getNavAnsattByNavIdent(navIdent: NavIdent, accessType: AccessType): AzureAdNavAnsatt? {
        return CacheUtils.tryCacheFirstNullable(ansattDataCacheByNavIdent, navIdent) {
            client.getNavAnsattByNavIdent(navIdent, accessType)
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

    suspend fun getNavAnsattSok(query: String, accessType: AccessType): List<AzureAdNavAnsatt> {
        return client.getNavAnsattSok(query, accessType)
    }

    suspend fun addToGroup(objectId: UUID, groupId: UUID) {
        return client.addToGroup(objectId, groupId)
    }
}
