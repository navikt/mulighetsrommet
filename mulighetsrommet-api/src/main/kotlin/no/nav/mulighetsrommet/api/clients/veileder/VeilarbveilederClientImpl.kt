package no.nav.mulighetsrommet.api.clients.veileder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.domain.VeilederDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarbveilederClientImpl::class.java)

class VeilarbveilederClientImpl(
    private val baseUrl: String,
    private val veilarbVeilederTokenProvider: AzureAdOnBehalfOfTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarbveilederClient {
    override suspend fun hentVeilederdata(accessToken: String?): VeilederDTO? {
        return try {
            client.get("$baseUrl/veileder/me") {
                bearerAuth(
                    veilarbVeilederTokenProvider.exchangeOnBehalfOfToken(
                        scope,
                        accessToken
                    )
                )
            }.body<VeilederDTO>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente data om veileder")
            null
        }
    }
}
