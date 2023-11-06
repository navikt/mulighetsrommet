package no.nav.mulighetsrommet.api.clients.person

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
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarbpersonClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) : VeilarbpersonClient {
    private val log = LoggerFactory.getLogger(javaClass)

    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val personInfoCache: Cache<String, PersonDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("personInfoCache", personInfoCache)
    }

    override suspend fun hentPersonInfo(fnr: String, accessToken: String): PersonDto {
        return CacheUtils.tryCacheFirstNotNull(personInfoCache, fnr) {
            try {
                val response = client.post("$baseUrl/v3/hent-person") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(PersonRequest(fnr = fnr))
                }

                if (!response.status.isSuccess()) {
                    SecureLog.logger.error("Klarte ikke hente persondata. Response: $response")
                    log.warn("Klarte ikke hente persondata. Se detaljer i SecureLog.")
                    throw NotFoundException("Fant ikke person")
                } else {
                    response.body()
                }
            } catch (exe: Exception) {
                SecureLog.logger.error("Klarte ikke hente persondata for bruker med fnr: $fnr", exe)
                log.error("Feil ved henting av persondata. Se detaljer i SecureLog.")
                throw exe
            }
        }
    }
}

@Serializable
data class PersonRequest(
    val fnr: String,
)
