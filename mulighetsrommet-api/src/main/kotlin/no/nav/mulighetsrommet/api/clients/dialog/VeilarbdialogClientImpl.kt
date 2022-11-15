package no.nav.mulighetsrommet.api.clients.dialog

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogResponse
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)
private val secureLog = SecureLog.logger

class VeilarbdialogClientImpl(
    private val baseUrl: String,
    private val veilarbdialogTokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbdialogClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }
    override suspend fun sendMeldingTilDialogen(
        fnr: String,
        accessToken: String,
        requestBody: DialogRequest
    ): DialogResponse? {
        return try {
            val response = client.post("$baseUrl/dialog?fnr=$fnr") {
                bearerAuth(
                    veilarbdialogTokenProvider.invoke(accessToken)
                )
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Conflict) {
                log.info("Kan ikke sende melding til dialogen, bruker oppfyller ikke kravene for digital kommunikasjon")
                return null
            }

            return response.body<DialogResponse>()
        } catch (exe: Exception) {
            secureLog.error("Klarte ikke sende melding til dialogen til bruker med fnr: $fnr", exe)
            log.error("Klarte ikke sende melding til dialogen. Se detaljer i secureLog.")
            null
        }
    }
}
