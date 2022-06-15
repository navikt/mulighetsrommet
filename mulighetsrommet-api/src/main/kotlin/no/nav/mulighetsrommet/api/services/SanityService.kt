package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.SanityConfig
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

class SanityService(sanity: SanityConfig) {
    private val logger = LoggerFactory.getLogger(SanityService::class.java)
    private val client: HttpClient
    private val sanityToken = sanity.authToken
    private val projectId = sanity.projectId
    private val apiVersion = SimpleDateFormat("yyyy-MM-dd").format(Date())
    private val sanityBaseUrl = "https://$projectId.apicdn.sanity.io/v$apiVersion/data/query/"

    init {
        logger.debug("Init SanityHttpClient")
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            expectSuccess = true
            defaultRequest {
                bearerAuth(sanityToken)
                url(sanityBaseUrl)
            }
        }
    }

    suspend fun executeQuery(query: String, dataset: String): JsonElement? {
        client.get {
            url {
                appendPathSegments(dataset)
                parameters.append("query", query)
            }
        }.let {
            val response = it.body<JsonObject>()
            return response["result"]
        }
    }
}
