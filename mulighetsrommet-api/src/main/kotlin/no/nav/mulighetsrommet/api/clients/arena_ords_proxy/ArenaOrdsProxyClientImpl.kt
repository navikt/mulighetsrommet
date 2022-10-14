package no.nav.mulighetsrommet.api.clients.arena_ords_proxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.domain.ArrangorDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ArenaOrdsProxyClient::class.java)

class ArenaOrdsProxyClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val scope: String
) : ArenaOrdsProxyClient {

    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }

    override suspend fun hentArbeidsgiver(arbeidsgiverId: Int): ArrangorDTO? {
        val response = client.get("$baseUrl/ords/arbeidsgiver") {
            bearerAuth(
                machineToMachineTokenClient.createMachineToMachineToken(
                    scope
                )
            )
            parameter("arbeidsgiverId", arbeidsgiverId)
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.warn("Fant ikke arrangørinfo for arrangør id: $arbeidsgiverId")
                null
            }
            else -> throw ResponseException(response, "Unexpected response from arena-ords-proxy")
        }
    }
}
