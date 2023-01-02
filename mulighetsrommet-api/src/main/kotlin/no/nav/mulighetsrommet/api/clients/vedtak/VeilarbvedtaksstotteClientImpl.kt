package no.nav.mulighetsrommet.api.clients.vedtak

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.domain.VedtakDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory

class VeilarbvedtaksstotteClientImpl(
    private val baseUrl: String,
    private val tokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbvedtaksstotteClient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    override suspend fun hentSiste14AVedtak(fnr: String, accessToken: String): VedtakDTO? {
        return try {
            val response = client.get("$baseUrl/siste-14a-vedtak?fnr=$fnr") {
                bearerAuth(tokenProvider.invoke(accessToken))
            }

            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.NoContent) {
                log.info("Fant ikke siste 14A-vedtak for bruker")
                return null
            }

            response.body<VedtakDTO>()
        } catch (exe: Exception) {
            SecureLog.logger.error("Klarte ikke hente siste 14A-vedtak for bruker med fnr: $fnr", exe)
            log.error("Klarte ikke hente siste 14A-vedtak. Se detaljer i secureLogs.")
            null
        }
    }
}
