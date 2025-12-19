package no.nav.mulighetsrommet.api.clients.dialog

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.teamLogs.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

class VeilarbdialogClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun sendMeldingTilDialogen(
        obo: AccessType.OBO,
        requestBody: DialogRequest,
    ): Either<VeilarbdialogError, DialogResponse> {
        val response = client.post("$baseUrl/dialog") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(requestBody)
        }

        return if (response.status.isSuccess()) {
            response.body<DialogResponse>().right()
        } else if (response.status == HttpStatusCode.Conflict) {
            log.info("Kan ikke sende melding til dialogen, bruker oppfyller ikke kravene for digital kommunikasjon")
            VeilarbdialogError.OppfyllerIkkeKravForDigitalKommunikasjon.left()
        } else {
            SecureLog.logger.error(
                "Klarte ikke sende melding til dialogen til bruker med fnr: ${requestBody.fnr.value}",
                response.bodyAsText(),
            )
            log.teamLogsError(
                "Klarte ikke sende melding til dialogen for bruker ${requestBody.fnr.value}",
                response.bodyAsText()
            )
            log.error("Klarte ikke sende melding til dialogen. Se detaljer i team logs.")
            VeilarbdialogError.Error.left()
        }
    }
}

enum class VeilarbdialogError {
    OppfyllerIkkeKravForDigitalKommunikasjon,
    Error,
}

@Serializable
data class DialogRequest(
    val fnr: NorskIdent,
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraBruker: Boolean,
)

@Serializable
data class DialogResponse(
    val id: String,
)
