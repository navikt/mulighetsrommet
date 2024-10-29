package no.nav.mulighetsrommet.api.clients.tiltakshistorikk

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkRequest
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkResponse
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider

class TiltakshistorikkClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun historikk(identer: List<NorskIdent>): TiltakshistorikkResponse {
        val response = client.post("$baseUrl/api/v1/historikk") {
            setBody(TiltakshistorikkRequest(identer, maxAgeYears = null))
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> throw ResponseException(response, "Unexpected response from tiltakshistorikk")
        }
    }
}
