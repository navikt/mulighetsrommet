package no.nav.mulighetsrommet.api.arenaadapter

import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.model.ExchangeArenaIdForIdResponse
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import java.util.UUID

class ArenaAdapterClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun exchangeTiltaksgjennomforingArenaIdForId(arenaId: String): ExchangeArenaIdForIdResponse? {
        val response = client.get("$baseUrl/api/exchange/$arenaId") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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
