package no.nav.mulighetsrommet.api.clients.ssb

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.clients.ClientResponseMetricPlugin
import org.slf4j.LoggerFactory

class SsbNusClient(engine: HttpClientEngine = CIO.create(), val config: Config) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class Input(
        val version: String,
    )

    data class Config(
        val baseUrl: String,
    )

    private val client: HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(ClientResponseMetricPlugin)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            exponentialDelay()
            modifyRequest {
                response?.let {
                    logger.warn("Request failed with response status=${it.status}")
                }
                logger.info("Retrying request method=${request.method.value}, url=${request.url.buildString()}")
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    internal suspend fun fetchNusData(version: String): SsbNusData {
        val response = client.get {
            url("${config.baseUrl}/klass/v1/versions/$version")
        }
        return response.body()
    }
}
