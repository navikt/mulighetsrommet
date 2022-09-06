package no.nav.mulighetsrommet.api.clients.person

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.domain.PersonDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)

class VeilarbpersonClientImpl(
    private val baseUrl: String,
    private val veilarbpersonTokenProvider: AzureAdOnBehalfOfTokenClient,
    private val scope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarbpersonClient {

    override suspend fun hentPersonInfo(fnr: String, accessToken: String?): PersonDTO? {
        return try {
            client.get("$baseUrl/v2/person?fnr=$fnr") {
                bearerAuth(
                    veilarbpersonTokenProvider.exchangeOnBehalfOfToken(
                        scope,
                        accessToken
                    )
                )
            }.body<PersonDTO>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente fornavn")
            null
        }
    }
}
