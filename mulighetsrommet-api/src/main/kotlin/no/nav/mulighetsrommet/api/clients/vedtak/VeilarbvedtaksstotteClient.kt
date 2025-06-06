package no.nav.mulighetsrommet.api.clients.vedtak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class VeilarbvedtaksstotteClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val siste14aVedtakCache: Cache<NorskIdent, Gjeldende14aVedtakDto> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentGjeldende14aVedtak(
        fnr: NorskIdent,
        obo: AccessType.OBO,
    ): Either<VedtakError, Gjeldende14aVedtakDto> {
        siste14aVedtakCache.getIfPresent(fnr)?.let { return@hentGjeldende14aVedtak it.right() }

        val response = client.post("$baseUrl/hent-gjeldende-14a-vedtak") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(VedtakRequest(fnr = fnr.value))
        }

        if (response.status == HttpStatusCode.Forbidden) {
            log.warn("Mangler tilgang til å hente siste 14A-vedtak for bruker. Har innlogget personen riktig AD-rolle for å hente siste 14A-vedtak?")
            return VedtakError.Forbidden.left()
        }

        if (!response.status.isSuccess()) {
            SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak. Response: $response")
            log.error("Klarte ikke hente siste 14A-vedtak. Status: ${response.status}")
            return VedtakError.Error.left()
        }

        val body = response.bodyAsText()
        if (body.isBlank()) {
            log.info("Fant ikke siste 14A-vedtak for bruker")
            return VedtakError.NotFound.left()
        }

        val vedtak = JsonIgnoreUnknownKeys.decodeFromString<Gjeldende14aVedtakDto>(body)
        siste14aVedtakCache.put(fnr, vedtak)
        return vedtak.right()
    }
}

enum class VedtakError {
    NotFound,
    Forbidden,
    Error,
}

@Serializable
data class VedtakRequest(
    val fnr: String,
)
