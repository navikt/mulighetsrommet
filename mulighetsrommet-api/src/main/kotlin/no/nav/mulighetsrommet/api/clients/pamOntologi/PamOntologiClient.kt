package no.nav.mulighetsrommet.api.clients.pamOntologi

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class PamOntologiClient(
    private val baseUrl: String,
    private val tokenProvider: (accessType: AccessType.OBO) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class Config(
        val baseUrl: String,
    )

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    suspend fun typeahead(query: String, domene: String, accessType: AccessType.OBO): List<Typeahead> {
        val response = client.get("$baseUrl/rest/typeahead/$domene?q=$query") {
            bearerAuth(tokenProvider.invoke(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Error fra pam-ontologi: $response")
        }
        return response.body()
    }
}

@Serializable
data class Typeahead(
    val konseptId: Long,
    val styrk08: String,
    val esco: String,
    val escoLabel: String,
    val label: String,
    val undertype: String = "",
)
