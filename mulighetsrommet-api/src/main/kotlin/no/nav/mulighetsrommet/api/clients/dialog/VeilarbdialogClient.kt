package no.nav.mulighetsrommet.api.clients.dialog

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogResponse
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.LoggerFactory

class VeilarbdialogClient(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun sendMeldingTilDialogen(
        fnr: String,
        accessToken: String,
        requestBody: DialogRequest,
    ): DialogResponse? {
        return try {
            val response = client.post("$baseUrl/dialog?fnr=$fnr") {
                bearerAuth(tokenProvider.invoke(accessToken))
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Conflict) {
                log.info("Kan ikke sende melding til dialogen, bruker oppfyller ikke kravene for digital kommunikasjon")
                return null
            }

            return response.body<DialogResponse>()
        } catch (exe: Exception) {
            SecureLog.logger.error("Klarte ikke sende melding til dialogen til bruker med fnr: $fnr", exe)
            log.error("Klarte ikke sende melding til dialogen. Se detaljer i secureLog.")
            null
        }
    }
}
