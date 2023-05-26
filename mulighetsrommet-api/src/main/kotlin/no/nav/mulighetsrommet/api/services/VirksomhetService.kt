package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VirksomhetService(
    private val brregClient: BrregClient,
    private val virksomhetRepository: VirksomhetRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
        return virksomhetRepository.get(orgnr).getOrThrow() ?: syncEnhetFraBrreg(orgnr)
    }

    suspend fun syncEnhetFraBrreg(orgnr: String): VirksomhetDto {
        val enhet = CacheUtils.tryCacheFirstNotNull(brregServiceCache, orgnr) {
            brregClient.hentEnhet(orgnr).getOrElse { throw it }
        }
        val overordnetEnhet = if (enhet.overordnetEnhet == null) {
            enhet
        } else {
            CacheUtils.tryCacheFirstNotNull(brregServiceCache, orgnr) {
                brregClient.hentEnhet(orgnr).getOrElse {
                    throw it
                }
            }
        }
        virksomhetRepository.upsert(overordnetEnhet)
            .onLeft { log.warn("Feil ved upsert av virksomhet: $it") }

        return enhet
    }

    suspend fun sokEtterEnhet(sokestreng: String): List<VirksomhetDto> {
        return brregClient.sokEtterOverordnetEnheter(sokestreng)
    }
}
