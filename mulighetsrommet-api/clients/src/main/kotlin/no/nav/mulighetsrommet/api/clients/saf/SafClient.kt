package no.nav.mulighetsrommet.api.clients.saf

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

/**
 * Klient mot SAF (Sak og arkiv) sitt GraphQL-API.
 *
 * Brukes til å slå opp journalposter, blant annet for å verifisere at en
 * oppgitt journalpostId finnes.
 */
class SafClient(
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
        install(HttpRequestRetry) {
            retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    suspend fun hentJournalpost(
        journalpostId: String,
        accessType: AccessType,
    ): Either<SafError, SafJournalpost> {
        val request = GraphqlRequest(
            query = JOURNALPOST_QUERY,
            variables = GraphqlRequest.JournalpostId(journalpostId = journalpostId),
        )

        val response = client.post("${config.baseUrl}/graphql") {
            bearerAuth(tokenProvider.exchange(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            log.error("Uventet svar fra saf: ${response.status}")
            return SafError.Error.left()
        }

        val graphqlResponse: GraphqlResponse<HentJournalpost> = try {
            response.body()
        } catch (e: Exception) {
            log.error("Kunne ikke deserialisere svar fra saf. Se teamlogs for detaljer.")
            log.teamLogsError("Kunne ikke deserialisere svar fra saf.", e)
            return SafError.Error.left()
        }

        if (graphqlResponse.errors.isNotEmpty()) {
            return if (graphqlResponse.errors.any { it.extensions?.code == SafErrorCode.NOT_FOUND }) {
                SafError.NotFound.left()
            } else {
                log.error("Feil fra saf: ${graphqlResponse.errors}")
                SafError.Error.left()
            }
        }

        return graphqlResponse.data?.journalpost?.right() ?: SafError.NotFound.left()
    }

    companion object {
        private val JOURNALPOST_QUERY = $$"""
            query($journalpostId: String!) {
                journalpost(journalpostId: $journalpostId) {
                    journalpostId
                }
            }
        """.trimIndent()
    }
}
