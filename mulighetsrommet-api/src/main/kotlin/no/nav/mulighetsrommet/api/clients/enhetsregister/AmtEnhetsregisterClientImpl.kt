package no.nav.mulighetsrommet.api.clients.enhetsregister

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AmtEnhetsregisterClient::class.java)

class AmtEnhetsregisterClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val scope: String
) : AmtEnhetsregisterClient {

    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }

    override suspend fun hentVirksomhet(virksomhetsnummer: Int): VirksomhetDTO? {
        val response = client.get("$baseUrl/api/enhet/$virksomhetsnummer") {
            bearerAuth(
                machineToMachineTokenClient.createMachineToMachineToken(
                    scope
                )
            )
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.warn("Virksomhet finnes ikke, sjekk securelogs")
                SecureLog.logger.warn("Virksomhet finnes ikke: $virksomhetsnummer")
                null
            }
            else -> throw ResponseException(response, "Unexpected response from amt-enhetsregister")
        }
    }
}
