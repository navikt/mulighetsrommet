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
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarboppfolgingClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) : VeilarboppfolgingClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val veilarboppfolgingCache: Cache<String, OppfolgingsstatusDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    private val manuellStatusCache: Cache<String, ManuellStatusDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("veilarboppfolgingCache", veilarboppfolgingCache)
        cacheMetrics.addCache("manuellStatusCache", manuellStatusCache)
    }

    override suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String): OppfolgingsstatusDto? {
        return CacheUtils.tryCacheFirstNotNull(veilarboppfolgingCache, fnr) {
            try {
                val response = client.get("$baseUrl/person/$fnr/oppfolgingsstatus") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                }

                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                    log.info("Fant ikke oppfølgingsstatus for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                    return null
                }

                response.body()
            } catch (exe: Exception) {
                SecureLog.logger.error("Klarte ikke hente oppfølgingsstatus for bruker med fnr: $fnr", exe)
                log.error("Klarte ikke hente oppfølgingsstatus. Se secureLogs for detaljer.")
                return null
            }
        }
    }

    override suspend fun hentManuellStatus(fnr: String, accessToken: String): ManuellStatusDto? {
        return CacheUtils.tryCacheFirstNotNull(manuellStatusCache, fnr) {
            try {
                val response = client.get("$baseUrl/v2/manuell/status?fnr=$fnr") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                }

                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                    log.info("Fant ikke manuell status for bruker.")
                    return null
                }

                response.body()
            } catch (exe: Exception) {
                SecureLog.logger.error("Klarte ikke hente manuell status for bruker med fnr: $fnr", exe)
                log.error("Klarte ikke hente manuell status. Se detaljer i secureLogs.")
                return null
            }
        }
    }
}
