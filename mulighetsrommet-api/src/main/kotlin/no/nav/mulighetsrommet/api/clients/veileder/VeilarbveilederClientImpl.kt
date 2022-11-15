package no.nav.mulighetsrommet.api.clients.veileder

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import no.nav.mulighetsrommet.api.domain.VeilederDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VeilarbveilederClientImpl::class.java)

class VeilarbveilederClientImpl(
    private val baseUrl: String,
    private val veilarbVeilederTokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbveilederClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }
    override suspend fun hentVeilederdata(accessToken: String): VeilederDTO? {
        return try {
            client.get("$baseUrl/veileder/me") {
                bearerAuth(
                    veilarbVeilederTokenProvider.invoke(accessToken)
                )
            }.body<VeilederDTO>()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente data om veileder")
            null
        }
    }
}
