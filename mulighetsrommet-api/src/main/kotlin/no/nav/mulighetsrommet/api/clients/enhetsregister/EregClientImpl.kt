package no.nav.mulighetsrommet.api.clients.enhetsregister

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.LoggerFactory

class EregClientImpl(
    private val baseUrl: String,
    clientEngine: HttpClientEngine = CIO.create(),
) : EregClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentVirksomhet(virksomhetsnummer: String): EregVirksomhetDto? {
        // TODO Vurder om inkluderHierarki skal være et filter vi kan konfigurere ved kall av tjeneste
        val response = client.get("$baseUrl/organisasjon/$virksomhetsnummer?inkluderHierarki=true") {
            headers {
                this.append("Nav-Consumer-Id", "team-mulighetsrommet")
            }
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.debug("Virksomhet finnes ikke i Ereg, sjekk securelogs")
                SecureLog.logger.debug("Virksomhet finnes ikke i Ereg: $virksomhetsnummer")
                null
            }

            else -> throw ResponseException(response, "Unexpected response from Ereg")
        }
    }
}
