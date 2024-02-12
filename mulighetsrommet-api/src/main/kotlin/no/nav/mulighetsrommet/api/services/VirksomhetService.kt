package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.domain.dbo.toOverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.metrics.Metrikker
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class VirksomhetService(
    private val brregClient: BrregClient,
    private val virksomhetRepository: VirksomhetRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val brregServiceCache: Cache<String, VirksomhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.HOURS)
        .maximumSize(20_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector = CacheMetricsCollector()
            .register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("brregServiceCache", brregServiceCache)
    }

    suspend fun getOrSyncVirksomhet(orgnr: String): Either<BrregError, VirksomhetDto> {
        return virksomhetRepository.get(orgnr).getOrThrow()?.right() ?: syncVirksomhetFraBrreg(orgnr)
    }

    suspend fun getVirksomhet(orgnr: String): Either<BrregError, VirksomhetDto> {
        val virksomhet = brregServiceCache.getIfPresent(orgnr)
        if (virksomhet != null) {
            return virksomhet.right()
        }

        // Sjekker først hovedenhet
        return brregClient.getHovedenhet(orgnr).fold(
            { error ->
                if (error == BrregError.NotFound) {
                    // Ingen treff på hovedenhet, vi sjekker underenheter også
                    brregClient.getUnderenhet(orgnr)
                } else {
                    error.left()
                }
            },
            {
                brregServiceCache.put(orgnr, it)
                it.right()
            },
        )
    }

    suspend fun syncVirksomhetFraBrreg(orgnr: String): Either<BrregError, VirksomhetDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return getVirksomhet(orgnr)
            .flatMap { virksomhet ->
                if (virksomhet.overordnetEnhet == null) {
                    virksomhet.right()
                } else {
                    log.info("Henter overordnet enhet fra brreg orgnr=$orgnr")
                    getVirksomhet(virksomhet.overordnetEnhet)
                }
            }
            .map { virksomhet ->
                if (virksomhet.slettetDato != null) {
                    log.info("Enhet med orgnr ${virksomhet.organisasjonsnummer} er slettet i Brreg med slettedato ${virksomhet.slettetDato}")
                    virksomhetRepository
                        .upsert(virksomhet)
                        .getOrThrow()
                } else {
                    virksomhetRepository
                        .upsertOverordnetEnhet(virksomhet.toOverordnetEnhetDbo())
                        .getOrThrow()
                }

                virksomhet
            }
    }

    fun upsertKontaktperson(kontaktperson: VirksomhetKontaktperson) =
        virksomhetRepository.upsertKontaktperson(kontaktperson)

    fun hentKontaktpersoner(orgnr: String): List<VirksomhetKontaktperson> =
        virksomhetRepository.getKontaktpersoner(orgnr)

    fun deleteKontaktperson(id: UUID): StatusResponse<Unit> {
        val (gjennomforinger, avtaler) = virksomhetRepository.koblingerTilKontaktperson(id)
        if (gjennomforinger.isNotEmpty()) {
            log.warn("Prøvde slette kontaktperson med koblinger til disse gjennomføringer: ${gjennomforinger.joinToString()}")
            return Either.Left(BadRequest("Kontaktpersonen er i bruk."))
        }
        if (avtaler.isNotEmpty()) {
            log.warn("Prøvde slette kontaktperson med koblinger til disse avtaler: ${avtaler.joinToString()}")
            return Either.Left(BadRequest("Kontaktpersonen er i bruk."))
        }

        return Either.Right(virksomhetRepository.deleteKontaktperson(id))
    }
}
