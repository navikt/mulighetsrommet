package no.nav.mulighetsrommet.api.clients.oppfolging

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
import no.nav.mulighetsrommet.utils.CacheUtils
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

    suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String): OppfolgingsstatusDto? {
        return CacheUtils.tryCacheFirstNullable(veilarboppfolgingCache, fnr) {
            try {
                val response = client.post("$baseUrl/v2/person/hent-oppfolgingsstatus") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(HentOppfolgingsstatusRequest(fnr = fnr))
                }

                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                    log.info("Fant ikke oppfølgingsstatus for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                    null
                } else if (!response.status.isSuccess()) {
                    log.warn("Klarte ikke hente oppfølgingsstatus for bruker. Status: ${response.status}")
                    null
                } else {
                    response.body()
                }
            } catch (exe: Exception) {
                log.error("Feil ved henting av oppfølgingsstatus for bruker", exe)
                null
            }
        }
    }

    suspend fun hentManuellStatus(fnr: String, accessToken: String): ManuellStatusDto? {
        return CacheUtils.tryCacheFirstNullable(manuellStatusCache, fnr) {
            try {
                val response = client.post("$baseUrl/v3/manuell/hent-status") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(ManuellStatusRequest(fnr = fnr))
                }

                if (!response.status.isSuccess()) {
                    log.warn("Klarte ikke hente manuell status for bruker. Status: ${response.status}")
                    null
                } else {
                    response.body()
                }
            } catch (exe: Exception) {
                log.error("Feil ved henting av manuell status for bruker", exe)
                null
            }
        }
    }
}

@Serializable
data class HentOppfolgingsstatusRequest(
    val fnr: String,
)

@Serializable
data class ManuellStatusRequest(
    val fnr: String,
)
