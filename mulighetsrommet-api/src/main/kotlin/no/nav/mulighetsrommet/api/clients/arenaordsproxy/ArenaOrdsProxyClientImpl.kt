package no.nav.mulighetsrommet.api.clients.arenaordsproxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.domain.ArbeidsgiverDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ArenaOrdsProxyClient::class.java)

class ArenaOrdsProxyClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : ArenaOrdsProxyClient {
    override suspend fun hentArbeidsgiver(arbeidsgiverId: Int): ArbeidsgiverDTO? {
        return try {
            val response = client.get("$baseUrl/ords/arbeidsgiver") {
                bearerAuth(
                    machineToMachineTokenClient.createMachineToMachineToken(
                        scope
                    )
                )
                parameter("arbeidsgiverId", arbeidsgiverId)
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
