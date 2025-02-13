package no.nav.tiltak.okonomi.oebs

import arrow.core.Either
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider

class OebsTiltakApiClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    suspend fun sendBestilling(bestilling: OebsBestillingMelding): Either<ResponseException, HttpResponse> {
        return request(HttpMethod.Post, "/api/v1/tilsagn", bestilling)
    }

    suspend fun sendAnnullering(annullering: OebsAnnulleringMelding): Either<ResponseException, HttpResponse> {
        return request(HttpMethod.Post, "/api/v1/tilsagn", annullering)
    }

    suspend fun sendFaktura(faktura: OebsFakturaMelding): Either<ResponseException, HttpResponse> {
        return request(HttpMethod.Post, "/api/v1/refusjonskrav", faktura)
    }

    private suspend inline fun <reified T> request(
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
