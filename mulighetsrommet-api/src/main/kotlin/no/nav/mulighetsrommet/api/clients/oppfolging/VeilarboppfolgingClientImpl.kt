package no.nav.mulighetsrommet.api.clients.oppfolging

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)

class VeilarboppfolgingClientImpl(
    private val baseUrl: String,
    private val veilarboppfolgingTokenProvider: suspend (String?) -> String?,
    private val client: HttpClient = baseClient()
) : VeilarboppfolgingClient {

    override suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String?): Oppfolgingsstatus? {
        try {
            val response =
                client.get("$baseUrl/person/$fnr/oppfolgingsstatus") {
                    header(HttpHeaders.Authorization, "Bearer ${veilarboppfolgingTokenProvider(accessToken)}")
                    header("Nav-Consumer-Id", "mulighetsrommet-api")
                }
            val data = response.body<JsonObject>()
            log.info(
                "Hentet oppfølgingsstatus for fnr: $fnr - Status: ${response.status} - Response: {}",
                data
            )
            return null
        } catch (exe: Exception) {
            log.error("Klarte ikke hente oppfølgingsstatus: {}", exe)
            return null
        }
    }
}

data class Oppfolgingsstatus(
    val oppfolgingsenhet: Oppfolgingsenhet,
    val veilederId: String,
    val formidlingsgruppe: String,
    val servicegruppe: String,
    val hovedmaalkode: String
)

data class Oppfolgingsenhet(
    val navn: String,
    val enhetId: String
)
