package no.nav.mulighetsrommet.api.clients.arenaadapter

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.domain.dto.ExchangeTiltaksnummerForIdResponse
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ArenaAdapterClientImpl::class.java)

class ArenaAdapterClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: () -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : ArenaAdaperClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun exchangeTiltaksnummerForUUID(tiltaksnummer: String): ExchangeTiltaksnummerForIdResponse? {
        val response = client.get("$baseUrl/api/exchange/$tiltaksnummer") {
            bearerAuth(
                machineToMachineTokenClient.invoke()
            )
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.warn("Tiltaksgjennomføring finnes ikke, sjekk securelogs")
                SecureLog.logger.warn("Tiltaksgjennomføring finnes ikke: $tiltaksnummer")
                null
            }
            else -> throw ResponseException(response, "Unexpected response from arena-adapter")
        }
    }
}
