package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class VirksomhetService(
    private val brregClient: BrregClient,
    private val virksomhetRepository: VirksomhetRepository,
) {
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
        val virksomhetFraDb = virksomhetRepository.get(orgnr).getOrThrow()
        if (virksomhetFraDb != null) {
            return virksomhetFraDb
        }

        val enhet = CacheUtils.tryCacheFirstNotNull(brregServiceCache, orgnr) {
            brregClient.hentEnhet(orgnr)
        }
        val overordnetEnhet = if (enhet.overordnetEnhet == null) {
            enhet
        } else {
            CacheUtils.tryCacheFirstNotNull(brregServiceCache, orgnr) {
                brregClient.hentEnhet(orgnr)
            }
        }
        virksomhetRepository.upsert(overordnetEnhet).getOrThrow()

        return enhet
    }

    suspend fun sokEtterEnhet(sokestreng: String): List<VirksomhetDto> {
        return brregClient.sokEtterOverordnetEnheter(sokestreng)
    }
}
