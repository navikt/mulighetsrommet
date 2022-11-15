package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.SanityConfig
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

private val log = LoggerFactory.getLogger(SanityService::class.java)

class SanityService(sanityConfig: SanityConfig, private val brukerService: BrukerService) {
    private val logger = LoggerFactory.getLogger(SanityService::class.java)
    private val client: HttpClient
    private val sanityToken = sanityConfig.authToken
    private val projectId = sanityConfig.projectId
    private val dataset = sanityConfig.dataset
    private val apiVersion = SimpleDateFormat("yyyy-MM-dd").format(Date())
    private val sanityBaseUrl = "https://$projectId.apicdn.sanity.io/v$apiVersion/data/query/$dataset"
    private val jsonDecoder = Json {
        ignoreUnknownKeys = true
    }
    private val fylkenummerCache = mutableMapOf<String?, String>()

    init {
        logger.debug("Init SanityHttpClient")
        client = httpJsonClient().config {
            defaultRequest {
                bearerAuth(sanityToken)
                url(sanityBaseUrl)
            }
        }
    }

    suspend fun executeQuery(query: String, fnr: String?, accessToken: String): JsonElement? {
        if (fnr !== null) {
            return getMedBrukerdata(query, fnr, accessToken)
        }
        return get(query)
    }

    private suspend fun getMedBrukerdata(query: String, fnr: String, accessToken: String): JsonElement? {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val fylkesId = getFylkeIdBasertPaaEnhetsId(brukerData.oppfolgingsenhet?.enhetId)
        return get(query, brukerData.oppfolgingsenhet?.enhetId ?: "", fylkesId)
    }

    private suspend fun get(query: String, enhetsId: String? = null, fylkeId: String? = null): JsonElement? {
        client.get {
            url {
                parameters.append("query", query)
                enhetsId?.let { parameters.append("\$enhetsId", "\"enhet.lokal.$it\"") }
                fylkeId?.let { parameters.append("\$fylkeId", "\"enhet.fylke.$it\"") }
            }
        }.let {
            val response = it.body<JsonObject>()
            return response["result"]
        }
    }

    private suspend fun getFylkeIdBasertPaaEnhetsId(enhetsId: String?): String {
        if (fylkenummerCache[enhetsId] != null) {
            return fylkenummerCache[enhetsId] ?: ""
        }

        val response = get("*[_type == \"enhet\" && type == \"Lokal\" && nummer.current == \"$enhetsId\"][0]{fylke->}")

        log.info("Hentet data om fylkeskontor basert på enhetsId: '$enhetsId' - Response: {}", response)

        return try {
            val fylkeResponse = response?.let { jsonDecoder.decodeFromJsonElement<FylkeResponse>(it) }
            val fylkeNummer = fylkeResponse?.fylke?.nummer?.current ?: ""
            if (fylkeNummer != "" && enhetsId != null) {
                fylkenummerCache[enhetsId] = fylkeNummer
            }
            fylkeNummer
        } catch (exception: Exception) {
            ""
        }
    }
}

@Serializable
private data class FylkeResponse(
    val fylke: Fylke
)

@Serializable
private data class Fylke(
    val nummer: Slug
)

@Serializable
private data class Slug(
    val current: String
)
