package no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class KontoregisterOrganisasjonClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {

    data class Config(
        val baseUrl: String,
        val tokenProvider: TokenProvider,
        val clientEngine: HttpClientEngine,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5)
            exponentialDelay()
        }
    }

    suspend fun getKontonummerForOrganisasjon(
        requestBody: KontonummerRequest,
    ): Either<KontonummerRegisterOrganisasjonError, KontonummerResponse> {
        val response = client.get("$baseUrl/kontoregister/api/v1/hent-kontonummer-for-organisasjon/${requestBody.organisasjonsnummer.value}") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(requestBody)
        }

        return if (response.status.isSuccess()) {
            response.body<KontonummerResponse>().right()
        } else if (response.status === HttpStatusCode.NotFound) {
            val error = response.body<Feilmelding>()
            SecureLog.logger.error(
                "Fant ikke orgnummer: ${requestBody.organisasjonsnummer.value} i kontoregisteret. Feilmelding: ${error.feilmelding}",
                response.bodyAsText(),
            )
            log.error("Fant ikke orgnummer for arrangør i kontoregisteret. Se detaljer i secureLog.")
            KontonummerRegisterOrganisasjonError.FantIkkeKontonummer.left()
        } else if (response.status === HttpStatusCode.MethodNotAllowed) {
            val error = response.body<Feilmelding>()
            SecureLog.logger.error(
                "Ugyldig input ved henting av kontonummer fra kontoregisteret. Feilmelding: ${error.feilmelding}",
                response.bodyAsText(),
            )
            log.error("Ugyldig input ved henting av kontonummer fra kontoregisteret. Se detaljer i secureLog.")
            KontonummerRegisterOrganisasjonError.UgyldigInput.left()
        } else {
            SecureLog.logger.error(
                "Klarte ikke hente kontonummer for arrangør: ${requestBody.organisasjonsnummer.value}",
                response.bodyAsText(),
            )
            log.error("Klarte ikke hente kontonummer for arrangør. Se detaljer i secureLog.")
            KontonummerRegisterOrganisasjonError.Error.left()
        }
    }
}

enum class KontonummerRegisterOrganisasjonError {
    FantIkkeKontonummer,
    UgyldigInput,
    Error,
}

@Serializable
data class KontonummerRequest(
    val organisasjonsnummer: Organisasjonsnummer,
)

@Serializable
data class KontonummerResponse(
    val mottaker: String,
    val kontonr: String,
)

@Serializable
data class Feilmelding(
    val feilmelding: String,
)
