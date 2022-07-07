package no.nav.mulighetsrommet.api.clients.oppfolging

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.Oppfolgingsstatus
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)

class VeilarboppfolgingClientImpl(
    private val baseUrl: String,
    private val veilarboppfolgingTokenProvider: suspend (String?) -> String?,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarboppfolgingClient {

    override suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String?): Oppfolgingsstatus? {
        return try {
            val response =
                client.get("$baseUrl/person/$fnr/oppfolgingsstatus") {
                    header(HttpHeaders.Authorization, "Bearer ${veilarboppfolgingTokenProvider(accessToken)}")
                    header("Nav-Consumer-Id", "mulighetsrommet-api")
                }
            response.body<Oppfolgingsstatus>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente oppfølgingsstatus: {}", exe.message, exe)
            null
        }
    }
}
