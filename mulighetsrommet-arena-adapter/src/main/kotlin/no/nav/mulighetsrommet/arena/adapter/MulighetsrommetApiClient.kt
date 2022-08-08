package no.nav.mulighetsrommet.arena.adapter

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.slf4j.LoggerFactory

class MulighetsrommetApiClient(uriBase: String, private val getToken: () -> String) {

    private val logger = LoggerFactory.getLogger(MulighetsrommetApiClient::class.java)
    private val client: HttpClient

    init {
        logger.debug("Init MulighetsrommetApiClient")

        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 5)
                exponentialDelay()
                modifyRequest {
                    response?.let {
                        logger.warn("Request failed with response status=${it.status}")
                    }
                    logger.info("Retrying request method=${request.method}, url=${request.url.buildString()}")
                }
            }
            defaultRequest {
                contentType(ContentType.Application.Json)

                url.takeFrom(
                    URLBuilder().takeFrom(uriBase).apply {
                        encodedPath += url.encodedPath
                    }
                )
            }
        }
    }

    internal suspend inline fun <reified T> sendRequest(method: HttpMethod, requestUri: String, payload: T) {
        val response: HttpResponse = client.request(requestUri) {
            bearerAuth(getToken())
            this.method = method
            setBody(payload)
        }

        if (!response.status.isSuccess()) {
            throw Exception("Request to mulighetsrommet-api failed with ${response.status}")
        }

        logger.debug("sent request status ${response.status} (${response.request.url})")
    }
}
