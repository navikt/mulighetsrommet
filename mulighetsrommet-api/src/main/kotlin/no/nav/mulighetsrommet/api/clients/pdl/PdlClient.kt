package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.common.client.pdl.Tema
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class PdlClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val pdlCache: Cache<String, List<IdentInformasjon>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentIdenter(ident: String): Either<PdlError, List<IdentInformasjon>> {
        pdlCache.getIfPresent(ident)?.let { return@hentIdenter it.right() }

        val response = graphqlRequest<GraphqlRequest.Ident, HentIdenterData>(
            GraphqlRequest(
                query = """
                    query(${'$'}ident: ID!) {
                        hentIdenter(ident: ${'$'}ident, historikk: true) {
                            identer {
                                ident,
                                historisk,
                                gruppe
                            }
                        }
                    }
                """.trimIndent(),
                variables = GraphqlRequest.Ident(ident = ident),
            ),
        )

        return if (response.errors.isNotEmpty()) {
            if (response.errors.any { error -> error.extensions?.code == "not_found" }) {
                PdlError.NotFound.left()
            } else {
                log.error("Error fra pdl ved hentIdenter: ${response.errors}")
                PdlError.Error.left()
            }
        } else {
            if (response.data.hentIdenter == null) {
                throw Exception("hentIdenter var null og errors tom! response: $response")
            }
            response.data.hentIdenter.identer.right()
        }
            .onRight { pdlCache.put(ident, it) }
    }

    private suspend inline fun <reified T, reified V> graphqlRequest(req: GraphqlRequest<T>): GraphqlResponse<V> {
        val response = client.post("$baseUrl/graphql") {
            bearerAuth(tokenProvider.invoke())
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Behandlingsnummer", "B450")
            header("Tema", Tema.GEN)
            setBody(req)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Error fra pdl ved hentIdenter: $response")
        }
        return response.body()
    }
}

enum class PdlError {
    Error,
    NotFound,
}

@Serializable
data class GraphqlRequest<T>(
    val query: String,
    val variables: T,
) {
    @Serializable
    data class Ident(
        val ident: String,
    )
}

@Serializable
data class GraphqlResponse<T>(
    val data: T,
    val errors: List<GraphqlError> = emptyList(),
) {
    @Serializable
    data class GraphqlError(
        val message: String? = null,
        val extensions: Extensions? = null,
    )

    @Serializable
    data class Extensions(
        val code: String,
    )
}

@Serializable
data class HentIdenterData(
    val hentIdenter: Identliste? = null,
) {

    @Serializable
    data class Identliste(
        val identer: List<IdentInformasjon>,
    )
}

@Serializable
data class IdentInformasjon(
    val ident: String,
    val gruppe: IdentGruppe,
    val historisk: Boolean,
)

@Serializable
enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
