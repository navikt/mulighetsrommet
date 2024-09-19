package no.nav.mulighetsrommet.api.clients.pamOntologi

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.net.URLEncoder

class PamOntologiClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
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

    suspend fun sokAutorisasjon(query: String): List<Typeahead> {
        return typeahead(query, "autorisasjon")
    }

    suspend fun sokAndreGodkjenninger(query: String): List<Typeahead> {
        return typeahead(query, "andre_godkjenninger")
    }

    private suspend fun typeahead(query: String, domene: String): List<Typeahead> {
        val urlEncodedQuery = withContext(Dispatchers.IO) {
            URLEncoder.encode(query, "UTF-8")
        }
        val response = client.get("$baseUrl/rest/typeahead/$domene?q=$urlEncodedQuery") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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
