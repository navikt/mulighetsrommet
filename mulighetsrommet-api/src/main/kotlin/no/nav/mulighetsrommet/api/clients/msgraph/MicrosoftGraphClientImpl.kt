package no.nav.mulighetsrommet.api.clients.msgraph

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
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

    override suspend fun hentAnsattdata(accessToken: String, navAnsattAzureId: UUID): AnsattDataDTO {
        val response = client.get("$baseUrl/v1.0/users/$navAnsattAzureId?\$select=streetAddress,city,givenName,surname,onPremisesSamAccountName") {
            bearerAuth(tokenProvider(accessToken))
        }

        if ((response.status == HttpStatusCode.NotFound) || (response.status == HttpStatusCode.NoContent)) {
            SecureLog.logger.warn("Klarte ikke finne bruker med azure-id: $navAnsattAzureId")
            log.error("Klarte ikke finne bruker med azure-id. Se detaljer i secureLog.")
            throw RuntimeException("Klarte ikke finne bruker med azure-id. Finnes brukeren i AD?")
        }

        val user = response.body<MSGraphUser>()
        return AnsattDataDTO(
            hovedenhetKode = user.streetAddress,
            hovedenhetNavn = user.city,
            fornavn = user.givenName,
            etternavn = user.surname,
            navident = user.onPremisesSamAccountName
        )
    }
}

@Serializable
data class MSGraphUser(
    val streetAddress: String,
    val city: String,
    val givenName: String,
    val surname: String,
    val onPremisesSamAccountName: String
)
