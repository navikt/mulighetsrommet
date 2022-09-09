package no.nav.mulighetsrommet.api.clients.poao_tilgang

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.setup.http.createHttpJsonClient
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class PoaoTilgangClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val client: HttpClient = createHttpJsonClient(engine)

    suspend fun hasAccessToModia(navIdent: String): Boolean {
        val response = client.post("$baseUrl/api/v1/tilgang/modia") {
            bearerAuth(tokenProvider.get())
            setBody(TilgangTilModiaRequest(navIdent))
        }

        if (!response.status.isSuccess()) {
            throw ResponseException(response, response.bodyAsText())
        }

        val decision = response.body<TilgangTilModiaResponse>().decision

        if (decision.type == Decision.DecisionType.DENY) {
            logger.warn("Bruker mangler tilgang til Modia Arbeidsrettet Oppfølging. navIdent=$navIdent, reason=${decision.reason}, message=${decision.message}")
        }

        return decision.type == Decision.DecisionType.PERMIT
    }

    @Serializable
    data class TilgangTilModiaRequest(
        val navIdent: String,
    )

    @Serializable
    data class TilgangTilModiaResponse(
        val decision: Decision,
    )

    @Serializable
    data class Decision(
        val type: DecisionType,
        val message: String? = null,
        val reason: String? = null,
    ) {
        enum class DecisionType {
            PERMIT, DENY
        }
    }
}
