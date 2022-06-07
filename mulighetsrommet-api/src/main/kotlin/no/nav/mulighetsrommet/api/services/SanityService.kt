package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.AppConfig
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

class SanityService(appConfig: AppConfig) {
    private val logger = LoggerFactory.getLogger(SanityService::class.java)
    private val client: HttpClient
    private val sanityToken = appConfig.sanity.authToken
    private val projectId = appConfig.sanity.projectId
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

    suspend fun executeQuery(query: String, dataset: String): String {
        val response: HttpResponse = client.get("") {
            url {
                appendPathSegments(dataset, "/")
                parameters.append("query", query)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        }

        return response.status.description
    }
}
