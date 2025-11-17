package no.nav.tiltak.historikk

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider

class TiltakshistorikkClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun getHistorikk(identer: List<NorskIdent>, years: Int? = null): TiltakshistorikkV1Response {
        val response = client.post("$baseUrl/api/v1/historikk") {
            setBody(TiltakshistorikkV1Request(identer, maxAgeYears = years))
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> throw ResponseException(response, "Unexpected response from tiltakshistorikk")
        }
    }
}
