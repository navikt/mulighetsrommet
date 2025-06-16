package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

/**
 * Team Valps behandlingsnummer for behandling av data for løsningen Arbeidsmarkedstiltak i Modia
 */
const val VALP_BEHANDLINGSNUMMER: String = "B450"

class PdlClient(
    private val config: Config,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    data class Config(
        val baseUrl: String,
        val maxRetries: Int = 0,
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    internal suspend inline fun <reified T, reified V> graphqlRequest(
        req: GraphqlRequest<T>,
        accessType: AccessType,
    ): Either<PdlError, V> {
        val response = client.post("${config.baseUrl}/graphql") {
            bearerAuth(tokenProvider.exchange(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Behandlingsnummer", VALP_BEHANDLINGSNUMMER)
            header("Tema", "GEN")
            setBody(req)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Error fra pdl: $response")
        }

        val graphqlResponse: GraphqlResponse<V> = try {
            response.body()
        } catch (e: Exception) {
            log.error("Kunne ikke deserialisere GraphqlResponse fra PDL. Se securelogs for mer informasjon.")
            SecureLog.logger.error("Kunne ikke deserialisere GraphqlResponse fra PDL.", e)
            return PdlError.Error.left()
        }

        return if (graphqlResponse.errors.isNotEmpty()) {
            if (graphqlResponse.errors.any { error -> error.extensions?.code == PdlErrorCode.NOT_FOUND }) {
                PdlError.NotFound.left()
            } else {
                log.error("Error fra pdl: ${graphqlResponse.errors}")
                PdlError.Error.left()
            }
        } else {
            val data = requireNotNull(graphqlResponse.data) {
                "forventet data siden errors var tom"
            }
            data.right()
        }
    }
}

enum class PdlError {
    Error,
    NotFound,
}

@Serializable
data class GraphqlRequest<T>(
    val query: String,
    val variables: T,
) {
    @Serializable
    data class Identer(
        val identer: Set<PdlIdent>,
    )

    @Serializable
    data class Ident(
        val ident: PdlIdent,
    )

    @Serializable
    data class HentHistoriskeIdenter(
        val ident: PdlIdent,
        val grupper: List<IdentGruppe>,
    )
}

@Serializable
data class GraphqlResponse<T>(
    val data: T? = null,
    val errors: List<GraphqlError> = emptyList(),
) {
    @Serializable
    data class GraphqlError(
        val message: String? = null,
        /**
         * Ekstra metadata relatert til feilen
         */
        val extensions: Extensions? = null,
    )

    @Serializable
    data class Extensions(
        /**
         * Feilkode fra PDL, kun til til stedet om dette er en feil spesifikt for PDL
         */
        val code: PdlErrorCode? = null,
        /**
         * Kategori av feilkode
         */
        val classification: String? = null,
    )
}

enum class PdlErrorCode {
    /**
     * Ikke gyldig token.
     */
    @SerialName("unauthenticated")
    UNAUTHENTICATED,

    /**
     * Gyldig, men feil type token eller ikke tilgang til tjenesten.
     */
    @SerialName("unauthorized")
    UNAUTHORIZED,

    /**
     * Fant ikke person i PDL.
     */
    @SerialName("not_found")
    NOT_FOUND,

    /**
     * Ugyldig ident. Ugyldig spørring. For stor bolkspørring.
     */
    @SerialName("bad_request")
    BAD_REQUEST,

    /**
     * Intern feil i Api.
     */
    @SerialName("server_error")
    SERVER_ERROR,
}
