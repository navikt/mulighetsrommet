package no.nav.mulighetsrommet.api.clients.tilgangsmaskin

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import no.nav.mulighetsrommet.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class TilgangsmaskinClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val logger = LoggerFactory.getLogger(TilgangsmaskinClient::class.java)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val tilgangCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun komplett(norskIdent: NorskIdent, obo: AccessType.OBO): Boolean = CacheUtils
        .tryCacheFirstNotNull(tilgangCache, "${obo.token}-${norskIdent.value}") {
            val response = client.post("$baseUrl/api/v1/komplett") {
                bearerAuth(tokenProvider.exchange(obo))
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(norskIdent.value)
            }

            return when (response.status) {
                HttpStatusCode.NoContent -> true

                HttpStatusCode.Forbidden -> false

                HttpStatusCode.NotFound -> {
                    logger.error("Nav Ident ikke funnet i tilgangsmaskinen. Dette burde ikke kunne skje")
                    throw Exception("Nav Ident ikke funnet i tilgangsmaskinen. Dette burde ikke kunne skje")
                }

                else -> {
                    logger.error("Feil mot tilgangsmaskinen. Status code: ${response.status}")
                    val bodyAsText = response.bodyAsText()
                    logger.teamLogsError("Feil mot tilgangsmaskinen. Response=$bodyAsText")
                    throw Exception("Feil mot tilgangsmaskinen. Status code: ${response.status}")
                }
            }
        }
}
