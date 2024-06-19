package no.nav.mulighetsrommet.api.clients.utdanning

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

class UtdanningClient(engine: HttpClientEngine = CIO.create(), val config: Config) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val baseurl: String,
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
            requestTimeoutMillis = 30000
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

    suspend fun getUtdanninger(): List<Utdanning> {
        val response = client.get("${config.baseurl}/api/v1/data_norge--utdanningsbeskrivelse")
        val utdanninger = response.body<List<String>>()
        return utdanninger.subList(0, 1).map { getUtdanning(it) } // TODO Ta med alle utdanninger
    }

    private suspend fun getUtdanning(url: String): Utdanning {
        val utdanning = client.get(url)
        return utdanning.body()
    }
}

@Serializable
data class Utdanning(
    val title: String,
    val utdtype: List<Utdanningstype>,
    val nus: List<Nuskodeverk>,
    val interesse: List<Interesse>,
) {
    @Serializable
    data class Utdanningstype(
        val title: String,
        val utdt_kode: String,
    )

    @Serializable
    data class Nuskodeverk(
        val nus_kode: String,
    )

    @Serializable
    data class Interesse(
        val title: String,
    )
}
