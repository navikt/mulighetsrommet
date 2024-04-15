package no.nav.mulighetsrommet.api.clients.sanity

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.domain.dto.Mutation
import no.nav.mulighetsrommet.api.domain.dto.Mutations
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.ktor.clients.ClientResponseMetricPlugin
import org.slf4j.LoggerFactory

class SanityClient(engine: HttpClientEngine = CIO.create(), val config: Config) {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val projectId: String,
        val dataset: String,
        // https://www.sanity.io/docs/api-versioning
        val apiVersion: String = "v2023-10-07",
        val token: String?,
        val useCdn: Boolean = true,
        val maxRetries: Int = 0,
    ) {
        private fun baseUrl(perspective: SanityPerspective = SanityPerspective.PUBLISHED): String {
            val api = if (useCdn && perspective != SanityPerspective.PREVIEW_DRAFTS) "apicdn" else "api"
            return "https://$projectId.$api.sanity.io/$apiVersion"
        }

        fun queryUrl(perspective: SanityPerspective) = "${baseUrl(perspective)}/data/query/$dataset"

        fun mutationUrl() = "${baseUrl()}/data/mutate/$dataset"
    }

    enum class MutationVisibility {
        Sync,
        Async,
        Deferred,
    }

    private val client: HttpClient = HttpClient(engine) {
        expectSuccess = true

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(ClientResponseMetricPlugin)

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = config.maxRetries)
            exponentialDelay()
            modifyRequest {
                response?.let {
                    logger.warn("Request failed with response status=${it.status}")
                }
                logger.info("Retrying request method=${request.method.value}, url=${request.url.buildString()}")
            }
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            config.token.takeIf {
                !it.isNullOrEmpty()
            }?.let {
                bearerAuth(it)
            }
        }
    }

    internal suspend fun query(
        query: String,
        perspective: SanityPerspective = SanityPerspective.PUBLISHED,
    ): SanityResponse {
        val response = client.get(config.queryUrl(perspective)) {
            url {
                parameters.append("query", query)
                parameters.append("perspective", perspective.navn)
            }
        }

        return response.body()
    }

    internal suspend inline fun <reified T> mutate(
        mutations: List<Mutation<T>>,
        returnIds: Boolean = false,
        returnDocuments: Boolean = false,
        dryRun: Boolean = false,
        visibility: MutationVisibility = MutationVisibility.Sync,
    ): HttpResponse {
        val response = client.post(config.mutationUrl()) {
            expectSuccess = false

            setBody(Mutations(mutations = mutations))
            url.parameters.apply {
                append("returnIds", returnIds.toString())
                append("returnDocuments", returnDocuments.toString())
                append("dryRun", dryRun.toString())
                append("visibility", visibility.name.lowercase())
            }
        }

        return response
    }
}

enum class SanityPerspective(val navn: String) {
    PREVIEW_DRAFTS("previewDrafts"),
    PUBLISHED("published"),
    RAW("raw"),
}
