package no.nav.mulighetsrommet.api.clients.person

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.domain.PersonDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)
private val secureLog = SecureLog.logger

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
            secureLog.error("Klarte ikke hente fornavn for bruker med fnr: $fnr")
            log.error("Klarte ikke hente fornavn. Se detaljer i secureLog.")
            null
        }
    }
}
