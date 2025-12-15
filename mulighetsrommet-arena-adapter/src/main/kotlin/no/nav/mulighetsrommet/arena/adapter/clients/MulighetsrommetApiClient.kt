package no.nav.mulighetsrommet.arena.adapter

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class MulighetsrommetApiClient(
    engine: HttpClientEngine,
    config: Config = Config(),
    baseUri: String,
    private val tokenProvider: TokenProvider,
) {

    data class Config(
        val maxRetries: Int = 0,
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient = httpJsonClient(engine).config {
        install(HttpRequestRetry) {
            retryIf(config.maxRetries) { _, response ->
                response.status.value.let { it in 500..599 } || response.status == HttpStatusCode.Conflict
            }

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

    internal suspend inline fun <reified T> request(
        method: HttpMethod,
        requestUri: String,
        payload: T? = null,
        isValidResponse: HttpResponse.() -> Boolean = { status.isSuccess() },
    ): Either<ResponseException, HttpResponse> {
        val response = client.request(requestUri) {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            this.method = method
            payload?.let { setBody(it) }
        }

        if (!isValidResponse(response)) {
            return Either.Left(ResponseException(response, response.bodyAsText()))
        }

        return Either.Right(response)
    }
}
