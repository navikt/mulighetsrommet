package no.nav.mulighetsrommet.api.clients.norg2

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class Norg2ClientImpl(
    private val baseUrl: String,
    clientEngine: HttpClientEngine = CIO.create()
) : Norg2Client {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine)
    override suspend fun hentEnheter(): List<Norg2EnhetDto> {
        return try {
            val response = client.get("$baseUrl/enhet")
            response.body()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente enheter fra NORG2. Konsekvensen er at oppdatering av enheter mot database ikke blir kjørt")
            throw exe
        }
    }
}
