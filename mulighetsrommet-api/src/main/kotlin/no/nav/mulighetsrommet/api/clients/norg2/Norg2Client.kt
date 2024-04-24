package no.nav.mulighetsrommet.api.clients.norg2

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class Norg2Client(
    private val baseUrl: String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout)
    }

    suspend fun hentEnheter(): List<Norg2Response> {
        return try {
            val response = client.get("$baseUrl/enhet/kontaktinformasjon/organisering/all") {
                headers {
                    this.append("consumerId", "team-mulighetsrommet-enhet-sync")
                }
            }
            response.body()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente enheter fra NORG2. Konsekvensen er at oppdatering av enheter mot database ikke blir kjørt")
            throw exe
        }
    }

    suspend fun hentEnhetByGeografiskOmraade(geografiskOmraade: String): Norg2EnhetDto {
        return try {
            val response = client.get("$baseUrl/enhet/navkontor/$geografiskOmraade") {
                headers {
                    this.append("consumerId", "team-mulighetsrommet")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                throw Exception("Not OK from Norg2, response: $response")
            }
            response.body()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente enhet basert på geografisk tilknytning fra NORG2. geografiskOmraade: $geografiskOmraade", exe)
            throw exe
        }
    }
}
