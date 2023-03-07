package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory

class SanityService(private val config: Config, private val brukerService: BrukerService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient
    private val fylkenummerCache = mutableMapOf<String?, String>()

    data class Config(
        val authToken: String?,
        val dataset: String,
        val projectId: String,
        val apiVersion: String = "v2023-01-01",
    ) {
        val apiUrl get() = "https://$projectId.apicdn.sanity.io/$apiVersion/data/query/$dataset"
    }

    init {
        logger.debug("Init SanityHttpClient")
        client = httpJsonClient().config {
            defaultRequest {
                config.authToken?.also {
                    bearerAuth(it)
                }

                url(config.apiUrl)
            }
        }
    }

    suspend fun executeQuery(query: String, fnr: String?, accessToken: String): SanityResponse {
        if (fnr !== null) {
            return getMedBrukerdata(query, fnr, accessToken)
        }
        return get(query)
    }

    suspend fun executeQuery(query: String): SanityResponse {
        return get(query)
    }

    suspend fun hentInnsatsgrupper(): SanityResponse {
        return executeQuery(
            """
            *[_type == "innsatsgruppe" && !(_id in path("drafts.**"))] | order(order asc)
            """.trimIndent()
        )
    }

    suspend fun hentTiltakstyper(): SanityResponse {
        return executeQuery(
            """
                *[_type == "tiltakstype" && !(_id in path("drafts.**"))]
            """.trimIndent()
        )
    }

    private suspend fun getMedBrukerdata(query: String, fnr: String, accessToken: String): SanityResponse {
        val brukerData = brukerService.hentBrukerdata(fnr, accessToken)
        val enhetsId = brukerData.oppfolgingsenhet?.enhetId ?: ""
        val fylkesId = getFylkeIdBasertPaaEnhetsId(brukerData.oppfolgingsenhet?.enhetId) ?: ""
        return get(query, enhetsId, fylkesId)
    }

    private suspend fun get(query: String, enhetsId: String? = null, fylkeId: String? = null): SanityResponse {
        client.get {
            url {
                parameters.append("query", query)
                enhetsId?.let { parameters.append("\$enhetsId", "\"enhet.lokal.$it\"") }
                fylkeId?.let { parameters.append("\$fylkeId", "\"enhet.fylke.$it\"") }
            }
        }.let {
            return it.body()
        }
    }

    private suspend fun getFylkeIdBasertPaaEnhetsId(enhetsId: String?): String? {
        if (fylkenummerCache[enhetsId] != null) {
            return fylkenummerCache[enhetsId]
        }

        val response = get("*[_type == \"enhet\" && type == \"Lokal\" && nummer.current == \"$enhetsId\"][0]{fylke->}")

        logger.info("Henter data om fylkeskontor basert på enhetsId: '$enhetsId' - Response: {}", response)

        val fylkeResponse = when (response) {
            is SanityResponse.Result -> response?.result?.let {
                JsonIgnoreUnknownKeys.decodeFromJsonElement<FylkeResponse>(
                    it
                )
            }

            else -> null
        }

        return try {
            val fylkeNummer = fylkeResponse?.fylke?.nummer?.current
            if (fylkeNummer != null && enhetsId != null) {
                fylkenummerCache[enhetsId] = fylkeNummer
            }
            fylkeNummer
        } catch (exception: Throwable) {
            logger.warn("Spørring mot Sanity feilet", exception)
            null
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

@Serializable(with = SanityReponseSerializer::class)
sealed class SanityResponse {
    @Serializable
    data class Result(
        val ms: Int,
        val query: String,
        val result: JsonElement?,
    ) : SanityResponse()

    @Serializable
    data class Error(
        val error: JsonObject,
    ) : SanityResponse()
}

object SanityReponseSerializer : JsonContentPolymorphicSerializer<SanityResponse>(SanityResponse::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "result" in element.jsonObject -> SanityResponse.Result.serializer()
        else -> SanityResponse.Error.serializer()
    }
}
