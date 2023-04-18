package no.nav.mulighetsrommet.api.clients.enhetsregister

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

class AmtEnhetsregisterClientImpl(
    private val baseUrl: String,
    private val tokenProvider: () -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : AmtEnhetsregisterClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentVirksomhet(virksomhetsnummer: String): VirksomhetDto? {
        val response = client.get("$baseUrl/api/enhet/$virksomhetsnummer") {
            bearerAuth(tokenProvider.invoke())
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.debug("Virksomhet finnes ikke, sjekk securelogs")
                SecureLog.logger.debug("Virksomhet finnes ikke: $virksomhetsnummer")
                null
            }

            else -> throw ResponseException(response, "Unexpected response from amt-enhetsregister")
        }
    }
}
