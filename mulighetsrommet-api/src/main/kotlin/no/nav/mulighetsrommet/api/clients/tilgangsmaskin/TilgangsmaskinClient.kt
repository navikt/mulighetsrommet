package no.nav.mulighetsrommet.api.clients.tilgangsmaskin

import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import kotlin.collections.List

/*
 * Confluence https://confluence.adeo.no/spaces/TM/pages/621546888/Tilgangsmaskin+API+og+regelsett
 * Swagger https://tilgangsmaskin.ansatt.dev.nav.no/swagger-ui/index.html#/TilgangController/bulkOBOForRegelType
 */
class TilgangsmaskinClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val logger = LoggerFactory.getLogger(TilgangsmaskinClient::class.java)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun bulk(identer: List<NorskIdent>, obo: AccessType.OBO.AzureAd): TilgangsmaskinResponse {
        if (identer.isEmpty()) {
            return TilgangsmaskinResponse(
                resultater = emptyList(),
            )
        }
        val response = client.post("$baseUrl/api/v1/bulk/obo") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            val body = identer.map { TilgangsmaskinRequest(brukerId = it.value) }
            logger.debug("tilgangsmaskin request: {}", body)
            setBody(identer.map { TilgangsmaskinRequest(brukerId = it.value) })
        }

        return when (response.status) {
            HttpStatusCode.MultiStatus -> {
                response.body<TilgangsmaskinResponse>()
            }

            HttpStatusCode.NotFound -> {
                logger.error("Nav Ident ikke funnet i tilgangsmaskinen. Dette burde ikke kunne skje")
                throw Exception("Nav Ident ikke funnet i tilgangsmaskinen. Dette burde ikke kunne skje")
            }

            HttpStatusCode.PayloadTooLarge -> {
                logger.error("For mange brukere i bulkrequest mot tilgangsmaskinen. Antall: ${identer.size}. Status code: ${response.status}")
                throw Exception("For mange brukere i bulkrequest mot tilgangsmaskinen. Status code: ${response.status}")
            }

            else -> {
                logger.error("Feil mot tilgangsmaskinen. Status code: ${response.status}")
                val bodyAsText = response.bodyAsText()
                logger.teamLogsError("Feil mot tilgangsmaskinen. Response=$bodyAsText")
                throw Exception("Feil mot tilgangsmaskinen. Status code: ${response.status}")
            }
        }
    }
}

@Serializable
data class TilgangsmaskinRequest(
    val brukerId: String,
    val type: Type = Type.KJERNE_REGELTYPE,
) {
    /* Komplett sjekk er med geografisk tilgangsjekk, kjerne er uten. */
    enum class Type {
        KOMPLETT_REGELTYPE,
        KJERNE_REGELTYPE,
    }
}

@Serializable
data class TilgangsmaskinResponse(
    val resultater: List<Resultat>,
) {
    @Serializable
    data class Resultat(
        val brukerId: String,
        val status: Int,
        val detaljer: ProblemDetail? = null,
    ) {
        fun harTilgang(): Boolean = status == 204
    }
}
