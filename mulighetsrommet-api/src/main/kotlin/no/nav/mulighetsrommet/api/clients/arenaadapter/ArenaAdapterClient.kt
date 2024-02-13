package no.nav.mulighetsrommet.api.clients.arenaadapter

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.domain.dto.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger(ArenaAdapterClient::class.java)

class ArenaAdapterClient(
    private val baseUrl: String,
    private val machineToMachineTokenClient: () -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun exchangeTiltaksgjennomforingsArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse? {
        val response = client.get("$baseUrl/api/exchange/$arenaId") {
            bearerAuth(
                machineToMachineTokenClient.invoke(),
            )
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.info("Tiltaksgjennomføring finnes ikke: $arenaId")
                null
            }
            else -> throw ResponseException(response, "Unexpected response from arena-adapter")
        }
    }

    suspend fun hentArenadata(id: UUID): ArenaTiltaksgjennomforingDto? {
        val response = client.get("$baseUrl/api/arenadata/$id") {
            bearerAuth(
                machineToMachineTokenClient.invoke(),
            )
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> {
                log.info("Tiltaksgjennomføring finnes ikke: $id")
                null
            }
            else -> throw ResponseException(response, "Unexpected response from arena-adapter")
        }
    }
}
