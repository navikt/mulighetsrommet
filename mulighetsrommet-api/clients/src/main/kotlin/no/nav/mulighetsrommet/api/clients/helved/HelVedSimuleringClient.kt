package no.nav.mulighetsrommet.api.clients.helved

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class HelVedSimuleringClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpTimeout) {
            requestTimeoutMillis = 121_000
        }
    }

    suspend fun simuler(
        request: HelVedUtbetaling,
        accessType: AccessType,
    ): Either<HelVedSimuleringsError, HelVedSimuleringResponse> {
        val response = client.post("$baseUrl/api/simulering") {
            bearerAuth(tokenProvider.exchange(accessType))
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<HelVedSimuleringResponse>().right()

            HttpStatusCode.BadRequest -> HelVedSimuleringsError.BadRequest.left()

            HttpStatusCode.NotFound -> HelVedSimuleringsError.NotFound.left()

            HttpStatusCode.Conflict -> HelVedSimuleringsError.Conflict.left()

            else -> {
                logger.error(
                    "Feil ved simulering mot Hel Ved. status={} id={} sakId={} behandlingId={}",
                    response.status,
                    request.id,
                    request.sakId,
                    request.behandlingId,
                )
                HelVedSimuleringsError.Error.left()
            }
        }
    }
}

enum class HelVedSimuleringsError {
    BadRequest,
    NotFound,
    Conflict,
    Error,
}
