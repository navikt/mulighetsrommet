package no.nav.mulighetsrommet.api.clients.person

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.domain.PersonDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)
private val secureLog = SecureLog.logger

class VeilarbpersonClientImpl(
    private val baseUrl: String,
    private val veilarbpersonTokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbpersonClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }
    override suspend fun hentPersonInfo(fnr: String, accessToken: String): PersonDTO? {
        return try {
            client.get("$baseUrl/v2/person?fnr=$fnr") {
                bearerAuth(
                    veilarbpersonTokenProvider.invoke(accessToken)
                )
            }.body<PersonDTO>()
        } catch (exe: Exception) {
            secureLog.error("Klarte ikke hente fornavn for bruker med fnr: $fnr")
            log.error("Klarte ikke hente fornavn. Se detaljer i secureLog.")
            null
        }
    }
}
