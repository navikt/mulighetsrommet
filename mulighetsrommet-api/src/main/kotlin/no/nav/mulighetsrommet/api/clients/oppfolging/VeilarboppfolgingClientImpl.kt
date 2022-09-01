package no.nav.mulighetsrommet.api.clients.oppfolging

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.domain.ManuellStatusDTO
import no.nav.mulighetsrommet.api.domain.Oppfolgingsstatus
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)

class VeilarboppfolgingClientImpl(
    private val baseUrl: String,
    private val veilarboppfolgingTokenProvider: AzureAdOnBehalfOfTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarboppfolgingClient {

    override suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String?): Oppfolgingsstatus? {
        return try {
            val response = client.get("$baseUrl/person/$fnr/oppfolgingsstatus") {
                bearerAuth(
                    veilarboppfolgingTokenProvider.exchangeOnBehalfOfToken(
                        scope,
                        accessToken
                    )
                )
                header("Nav-Consumer-Id", "mulighetsrommet-api")
            }

            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                log.info("Fant ikke oppfølgingsstatus for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                return null
            }

            response.body<Oppfolgingsstatus>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente oppfølgingsstatus: {}", exe.message)
            null
        }
    }

    override suspend fun hentManuellStatus(fnr: String, accessToken: String?): ManuellStatusDTO? {
        return try {
            val response = client.get("$baseUrl/v2/manuell/status?fnr=$fnr") {
                bearerAuth(
                    veilarboppfolgingTokenProvider.exchangeOnBehalfOfToken(
                        scope,
                        accessToken
                    )
                )
                header("Nav-Consumer-Id", "mulighetsrommet-api")
            }

            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                log.info("Fant ikke manuell status for bruker.")
                return null
            }

            val body = response.body<ManuellStatusDTO>()
            body
        } catch (exe: Exception) {
            log.error("Klarte ikke hente manuell status")
            null
        }
    }
}
