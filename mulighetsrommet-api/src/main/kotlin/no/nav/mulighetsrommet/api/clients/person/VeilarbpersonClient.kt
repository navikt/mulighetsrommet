package no.nav.mulighetsrommet.api.clients.person

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
import io.ktor.server.plugins.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.metrics.Metrikker
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarbpersonClient(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val behandlingsnummer = "B450" // Team Valps behandlingsnummer for behandling av data for løsningen Arbeidsmarkedstiltak i Modia

    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val personInfoCache: Cache<String, PersonDto> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    init {
        val cacheMetrics: CacheMetricsCollector =
            CacheMetricsCollector().register(Metrikker.appMicrometerRegistry.prometheusRegistry)
        cacheMetrics.addCache("personInfoCache", personInfoCache)
    }

    suspend fun hentPersonInfo(fnr: String, accessToken: String): Either<PersonError, PersonDto> {
        personInfoCache.getIfPresent(fnr)?.let { return@hentPersonInfo it.right() }

        val response = client.post("$baseUrl/v3/hent-person") {
            bearerAuth(tokenProvider.invoke(accessToken))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(PersonRequest(fnr = fnr, behandlingsnummer = behandlingsnummer))
        }

        return if (response.status == HttpStatusCode.Forbidden) {
            log.warn("Manglet tilgang til å hente persondata.")
            PersonError.Forbidden.left()
        } else {
            if (!response.status.isSuccess()) {
                SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak. Response: $response")
                log.warn("Klarte ikke hente persondata. Status: ${response.status}.")
                PersonError.Error.left()
            } else {
                response.body<PersonDto>().right()
            }
                .onRight { personInfoCache.put(fnr, it) }
        }
    }
}

enum class PersonError {
    Forbidden,
    Error,
}

@Serializable
data class PersonRequest(
    val fnr: String,
    val behandlingsnummer: String,
)
