package no.nav.mulighetsrommet.api.clients.amtenhetsregister

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AmtEnhetsregisterClient::class.java)

class AmtEnhetsregisterClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : AmtEnhetsregisterClient {
    override suspend fun hentVirksomhetsNavn(virksomhetsnummer: Int): VirksomhetDTO? {
        return try {
            val response = client.get("$baseUrl/api/enhet/$virksomhetsnummer") {
                bearerAuth(
                    machineToMachineTokenClient.createMachineToMachineToken(
                        scope
                    )
                )
            }
            if (response.status == HttpStatusCode.OK) {
                return response.body()
            }
            null
        } catch (exe: Exception) {
            log.error("Klarte ikke hente arbeidsgiverinfo")
            null
        }
    }
}
