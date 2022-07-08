package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.SanityConfig
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

private val log = LoggerFactory.getLogger(SanityService::class.java)

class SanityService(sanityConfig: SanityConfig, brukerService: BrukerService) {
    private val logger = LoggerFactory.getLogger(SanityService::class.java)
    private val client: HttpClient
    private val sanityToken = sanityConfig.authToken
    private val projectId = sanityConfig.projectId
    private val dataset = sanityConfig.dataset
    private val apiVersion = SimpleDateFormat("yyyy-MM-dd").format(Date())
    private val sanityBaseUrl = "https://$projectId.apicdn.sanity.io/v$apiVersion/data/query/$dataset"
    private val brukerService = brukerService

    init {
        logger.debug("Init SanityHttpClient")
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            expectSuccess = true
            defaultRequest {
                bearerAuth(sanityToken)
                url(sanityBaseUrl)
            }
        }
    }

    suspend fun executeQuery(query: String, fnr: String?, accessToken: String?): JsonElement? {
        var brukerData: Brukerdata?
        if (fnr !== null) {
            // TODO Bruk brukerData til å filtrere - Kommer via Signes branch senere
            brukerData = brukerService.hentBrukerdata(fnr, accessToken)
            log.info(
                "Hentet brukerdata som trengs for videre filtrering mot Sanity.\nInnsatsgruppe: {}\nOppfølgingsstatus: {}",
                brukerData.innsatsgruppe,
                brukerData.oppfolgingsenhet
            )
        }
        client.get {
            url {
                parameters.append("query", query)
            }
        }.let {
            val response = it.body<JsonObject>()
            return response["result"]
        }
    }
}
