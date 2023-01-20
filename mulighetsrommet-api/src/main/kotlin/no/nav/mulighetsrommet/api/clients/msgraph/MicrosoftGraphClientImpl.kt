package no.nav.mulighetsrommet.api.clients.msgraph

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.MSGraphBrukerdata
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory
import java.util.*

class MicrosoftGraphClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : MicrosoftGraphClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentHovedenhetForBruker(accessToken: String, navAnsattAzureId: UUID): MSGraphBrukerdata {
        val response = client.get("$baseUrl/v1.0/users/$navAnsattAzureId?\$select=streetAddress,city") {
            bearerAuth(tokenProvider(accessToken))
        }

        if ((response.status == HttpStatusCode.NotFound) || (response.status == HttpStatusCode.NoContent)) {
            SecureLog.logger.warn("Klarte ikke finne bruker med azure-id: $navAnsattAzureId")
            log.error("Klarte ikke finne bruker med azure-id. Se detaljer i secureLog.")
            throw RuntimeException("Klarte ikke finne bruker med azure-id. Finnes brukeren i AD?")
        }

        val user = response.body<MSGraphUser>()
        return MSGraphBrukerdata(
            hovedenhetKode = user.streetAddress,
            hovedenhetNavn = user.city
        )
    }
}

@kotlinx.serialization.Serializable
data class MSGraphUser(
    val streetAddress: String,
    val city: String
)
