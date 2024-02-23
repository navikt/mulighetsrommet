package no.nav.mulighetsrommet.api.clients.oppfolging

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.metrics.Metrikker
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarboppfolgingClient(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val veilarboppfolgingCache: Cache<String, OppfolgingsstatusDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    private val manuellStatusCache: Cache<String, ManuellStatusDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("veilarboppfolgingCache", veilarboppfolgingCache)
        cacheMetrics.addCache("manuellStatusCache", manuellStatusCache)
    }

    suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String): Either<OppfolgingsstatusError, OppfolgingsstatusDto> {
        veilarboppfolgingCache.getIfPresent(fnr)?.let { return@hentOppfolgingsstatus it.right() }

        val response = client.post("$baseUrl/v2/person/hent-oppfolgingsstatus") {
            bearerAuth(tokenProvider.invoke(accessToken))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(HentOppfolgingsstatusRequest(fnr = fnr))
        }

        return when (response.status) {
            HttpStatusCode.NotFound -> {
                log.info("Fant ikke oppfølgingsstatus for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                OppfolgingsstatusError.NotFound.left()
            }
            HttpStatusCode.Forbidden -> {
                log.info("Manglet tilgang til å hente oppfølgingsstatus for bruker.")
                OppfolgingsstatusError.Forbidden.left()
            }
            else -> if (!response.status.isSuccess()) {
                log.warn("Klarte ikke hente oppfølgingsstatus for bruker. Status: ${response.status}")
                OppfolgingsstatusError.Error.left()
            } else {
                response.body<OppfolgingsstatusDto>().right()
            }
                .onRight { veilarboppfolgingCache.put(fnr, it) }
        }
    }

    suspend fun hentManuellStatus(fnr: String, accessToken: String): Either<ManuellStatusError, ManuellStatusDto> {
        manuellStatusCache.getIfPresent(fnr)?.let { return@hentManuellStatus it.right() }

        val response = client.post("$baseUrl/v3/manuell/hent-status") {
            bearerAuth(tokenProvider.invoke(accessToken))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ManuellStatusRequest(fnr = fnr))
        }

        return if (response.status == HttpStatusCode.Forbidden) {
            log.warn("Manglet tilgang til å hente manuell status for bruker")
            ManuellStatusError.Forbidden.left()
        } else {
            if (!response.status.isSuccess()) {
                log.warn("Klarte ikke hente manuell status for bruker. Status: ${response.status}")
                ManuellStatusError.Error.left()
            } else {
                response.body<ManuellStatusDto>().right()
            }
                .onRight { manuellStatusCache.put(fnr, it) }
        }
    }
}

enum class OppfolgingsstatusError {
    Forbidden,
    NotFound,
    Error,
}

enum class ManuellStatusError {
    Forbidden,
    Error,
}

@Serializable
data class HentOppfolgingsstatusRequest(
    val fnr: String,
)

@Serializable
data class ManuellStatusRequest(
    val fnr: String,
)
