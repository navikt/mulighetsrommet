package no.nav.mulighetsrommet.api.clients.arena

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarbarenaClientImpl::class.java)

class VeilarbarenaClientImpl(
    private val baseUrl: String,
    private val veilarbArenaTokenProvider: AzureAdOnBehalfOfTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarbarenaClient {
    override suspend fun hentPersonIdForFnr(fnr: String, accessToken: String?): String? {
        return try {
            val response = client.get("$baseUrl/hentPersonId") {
                bearerAuth(
                    veilarbArenaTokenProvider.exchangeOnBehalfOfToken(
                        scope,
                        accessToken
                    )
                )
                header("Nav-Consumer-Id", "mulighetsrommet-api")
                parameter("fnr", fnr)
            }

            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                log.info("Fant ikke personId for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                return null
            }

            response.bodyAsText()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente personId for bruker: {}", exe.message)
            null
        }
    }
}
