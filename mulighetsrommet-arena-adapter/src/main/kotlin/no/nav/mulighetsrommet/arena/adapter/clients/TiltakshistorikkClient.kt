package no.nav.mulighetsrommet.arena.adapter.clients

import arrow.core.Either
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class TiltakshistorikkClient(
    engine: HttpClientEngine = CIO.create(),
    config: Config = Config(),
    baseUri: String,
    private val getToken: () -> String,
) {
    data class Config(
        val maxRetries: Int = 0,
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient

    init {
        client = httpJsonClient(engine).config {
            install(HttpRequestRetry) {
                retryOnException(config.maxRetries, retryOnTimeout = true)
                exponentialDelay()

                modifyRequest {
                    response?.let {
                        logger.info("Request failed with response_status=${it.status}")
                    }
                    logger.info("Retrying request method=${request.method.value}, url=${request.url.buildString()}")
                }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)

                url.takeFrom(
                    URLBuilder().takeFrom(baseUri).apply {
                        encodedPath += url.encodedPath
                    },
                )
            }
        }
    }

    internal suspend inline fun <reified T> request(
        method: HttpMethod,
        requestUri: String,
        payload: T? = null,
        isValidResponse: HttpResponse.() -> Boolean = { status.isSuccess() },
    ): Either<ResponseException, HttpResponse> {
        val response = client.request(requestUri) {
            bearerAuth(getToken())
            this.method = method
            payload?.let { setBody(it) }
        }

        if (!isValidResponse(response)) {
            return Either.Left(ResponseException(response, response.bodyAsText()))
        }

        return Either.Right(response)
    }
}
