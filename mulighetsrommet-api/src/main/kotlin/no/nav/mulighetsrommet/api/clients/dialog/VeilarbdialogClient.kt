package no.nav.mulighetsrommet.api.clients.dialog

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.TokenProvider
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.LoggerFactory

class VeilarbdialogClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun sendMeldingTilDialogen(
        obo: AccessType.OBO,
        requestBody: DialogRequest,
    ): DialogResponse? {
        return try {
            val response = client.post("$baseUrl/dialog") {
                bearerAuth(tokenProvider.exchange(obo))
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Conflict) {
                log.info("Kan ikke sende melding til dialogen, bruker oppfyller ikke kravene for digital kommunikasjon")
                return null
            }

            return response.body<DialogResponse>()
        } catch (exe: Exception) {
            SecureLog.logger.error("Klarte ikke sende melding til dialogen til bruker med fnr: ${requestBody.fnr}", exe)
            log.error("Klarte ikke sende melding til dialogen. Se detaljer i secureLog.")
            null
        }
    }
}

@Serializable
data class DialogRequest(
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraBruker: Boolean,
    val fnr: String,
)

@Serializable
data class DialogResponse(
    val id: String,
)
