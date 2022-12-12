package no.nav.mulighetsrommet.api.clients.msgraph

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger(MicrosoftGraphClientImpl::class.java)
private val secureLog = SecureLog.logger

class MicrosoftGraphClientImpl(
    private val baseUrl: String,
    private val tokenProvider: () -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : MicrosoftGraphClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentHovedenhetForBruker(navAnsattAzureId: UUID): String {
        val response = client.get("$baseUrl/v1.0/users/$navAnsattAzureId?\$select=streetAddress") {
            bearerAuth(tokenProvider())
        }

        if ((response.status == HttpStatusCode.NotFound) || (response.status == HttpStatusCode.NoContent)) {
            secureLog.warn("Klarte ikke finne bruker med azure-id: $navAnsattAzureId")
            throw RuntimeException("Klarte ikke finne bruker med azure-id. Finnes brukeren i AD?")
        }

        val user = response.body<MSGraphUser>()
        return user.streetAddress // Hovedenhet finnes på streetAddress-egenskapen til bruker
    }
}

@kotlinx.serialization.Serializable
data class MSGraphUser(
    val streetAddress: String
)
