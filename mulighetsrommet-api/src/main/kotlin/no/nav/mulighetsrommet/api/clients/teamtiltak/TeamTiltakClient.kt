package no.nav.mulighetsrommet.api.clients.teamtiltak

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

class TeamTiltakClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: (String) -> String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(engine).config {
        install(HttpCache)
    }

    suspend fun getAvtaler(norskIdent: String, accessToken: String): List<String> {
        return try {
            val bearerAuth = tokenProvider(accessToken)
            log.warn("Accesstoekn: $bearerAuth")
            val response = client.post("$baseUrl/tiltaksgjennomforing-api/avtale-hendelse/$norskIdent") {
                bearerAuth(bearerAuth)
            }

            if (!response.status.isSuccess()) {
                log.error("Feil mot tiltaksgjennomforing-api status: ${response.status}, response: $response")
                throw RuntimeException("Feil mot tiltaksgjennomforing-api status: ${response.status}, response: $response")
            }
            log.warn("Response fra team tiltak status: ${response.status}, response: $response")

            response.body()
        } catch (t: Throwable) {
            log.error("exception ved tiltaksgjennomforing-api $t, ${t.message}")
            emptyList()
        }
    }
}
