package no.nav.mulighetsrommet.api.clients.vedtak

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarbvedtaksstotteClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) : VeilarbvedtaksstotteClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val siste14aVedtakCache: Cache<String, VedtakDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("siste14aVedtakCache", siste14aVedtakCache)
    }

    override suspend fun hentSiste14AVedtak(fnr: String, accessToken: String): VedtakDto? {
        return CacheUtils.tryCacheFirstNullable(siste14aVedtakCache, fnr) {
            try {
                val response = client.get("$baseUrl/siste-14a-vedtak?fnr=$fnr") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                }

                if (response.status == HttpStatusCode.NotFound) {
                    log.info("Fant ikke siste 14A-vedtak for bruker")
                    return@tryCacheFirstNullable null
                } else if (response.status == HttpStatusCode.Forbidden) {
                    log.warn("Mangler tilgang til å hente siste 14A-vedtak for bruker. Har innlogget personen riktig AD-rolle for å hente siste 14A-vedtak?")
                    return@tryCacheFirstNullable null
                } else if (!response.status.isSuccess()) {
                    SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak. Response: $response")
                    log.error("Klarte ikke hente siste 14A-vedtak. Se detaljer i SecureLog.")
                    return@tryCacheFirstNullable null
                }

                val body = response.bodyAsText()
                if (body.isBlank()) {
                    log.info("Fant ikke siste 14A-vedtak for bruker")
                    null
                } else {
                    JsonIgnoreUnknownKeys.decodeFromString(body)
                }
            } catch (e: Throwable) {
                SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak for bruker med fnr: $fnr", e)
                log.error("Klarte ikke hente siste 14A-vedtak. Se detaljer i SecureLog.")
                null
            }
        }
    }
}
