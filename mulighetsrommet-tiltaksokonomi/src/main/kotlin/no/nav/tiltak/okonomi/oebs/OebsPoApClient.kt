package no.nav.tiltak.okonomi.oebs

import arrow.core.Either
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class OebsPoApClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    companion object {
        const val BESTILLING_ENDPOINT = "/api/v1/bestilling"
        const val FAKTURA_ENDPOINT = "/api/v1/faktura"
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    suspend fun sendBestilling(bestilling: OebsBestillingMelding): Either<Throwable, HttpResponse> {
        return request(HttpMethod.Post, BESTILLING_ENDPOINT, bestilling)
    }

    suspend fun sendAnnullering(annullering: OebsAnnulleringMelding): Either<Throwable, HttpResponse> {
        return request(HttpMethod.Post, BESTILLING_ENDPOINT, annullering)
    }

    suspend fun sendFaktura(faktura: OebsFakturaMelding): Either<Throwable, HttpResponse> {
        return request(HttpMethod.Post, FAKTURA_ENDPOINT, faktura)
    }

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        requestUri: String,
        payload: T? = null,
        isValidResponse: HttpResponse.() -> Boolean = { status.isSuccess() },
    ): Either<Throwable, HttpResponse> {
        val response = try {
            client.request(requestUri) {
                bearerAuth(tokenProvider.exchange(AccessType.M2M))
                this.method = method
                payload?.let { setBody(it) }
            }
        } catch (e: Exception) {
            log.error("Request \"${method.value} $requestUri\" failed. request body=${Json.encodeToString(payload)}", e)
            return Either.Left(e)
        }

        if (!isValidResponse(response)) {
            val requestBody = Json.encodeToString(payload)
            val responseBody = response.bodyAsText()
            log.warn("Request \"${method.value} $requestUri\" failed. status=${response.status}, request body=$requestBody, response body=$responseBody")
            return Either.Left(ResponseException(response, responseBody))
        }

        return Either.Right(response)
    }
}
