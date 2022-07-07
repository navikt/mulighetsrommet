package no.nav.mulighetsrommet.api.clients.vedtak

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClientImpl
import no.nav.mulighetsrommet.api.domain.VedtakDTO
import no.nav.mulighetsrommet.api.setup.http.baseClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarboppfolgingClientImpl::class.java)

class VeilarbvedtaksstotteClientImpl(
    private val baseUrl: String,
    private val veilarbvedtaksstotteTokenProvider: suspend (String?) -> String?,
    private val client: HttpClient = baseClient().config {
        install(HttpCache)
    }
) : VeilarbvedtaksstotteClient {

    override suspend fun hentSiste14AVedtak(fnr: String, accessToken: String?): VedtakDTO? {
        return try {
            val response =
                client.get("$baseUrl/siste-14a-vedtak?fnr=$fnr") {
                    header(HttpHeaders.Authorization, "Bearer ${veilarbvedtaksstotteTokenProvider(accessToken)}")
                    header("Nav-Consumer-Id", "mulighetsrommet-api")
                }
            response.body<VedtakDTO>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente siste 14a-vedtak: {}", exe.message, exe)
            null
        }
    }
}
