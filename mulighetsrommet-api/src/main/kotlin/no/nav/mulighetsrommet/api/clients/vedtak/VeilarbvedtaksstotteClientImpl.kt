package no.nav.mulighetsrommet.api.clients.vedtak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.domain.VedtakDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.secure_log.SecureLog
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarbvedtaksstotteClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbvedtaksstotteClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val siste14aVedtakCache: Cache<String, VedtakDTO> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("siste14aVedtakCache", siste14aVedtakCache)
    }

    override suspend fun hentSiste14AVedtak(fnr: String, accessToken: String): VedtakDTO? {
        return CacheUtils.tryCacheFirstNotNull(siste14aVedtakCache, fnr) {
            return try {
                val response = client.get("$baseUrl/siste-14a-vedtak?fnr=$fnr") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                }

                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                    log.info("Fant ikke siste 14A-vedtak for bruker")
                    return null
                }

                response.body()
            } catch (exe: Exception) {
                SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak for bruker med fnr: $fnr", exe)
                log.error("Klarte ikke hente siste 14A-vedtak. Se detaljer i secureLogs.")
                return null
            }
        }
    }
}
