package no.nav.mulighetsrommet.api.clients.veileder

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import no.nav.mulighetsrommet.api.domain.VeilederDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class VeilarbveilederClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbveilederClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val cache: Cache<UUID, VeilederDTO> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("veilederCache", cache)
    }

    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentVeilederdata(accessToken: String, navAnsattAzureId: UUID): VeilederDTO? {
        return CacheUtils.tryCacheFirstNotNull(cache, navAnsattAzureId) {
            try {
                client.get("$baseUrl/veileder/me") {
                    bearerAuth(tokenProvider.invoke(accessToken))
                }.body()
            } catch (exe: Exception) {
                log.error("Klarte ikke hente data om veileder")
                return null
            }
        }
    }
}
