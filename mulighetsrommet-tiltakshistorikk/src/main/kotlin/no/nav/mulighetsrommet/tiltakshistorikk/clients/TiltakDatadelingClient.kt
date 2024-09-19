package no.nav.mulighetsrommet.tiltakshistorikk.clients

import arrow.core.Either
import arrow.core.Nel
import arrow.core.left
import arrow.core.right
import arrow.core.serialization.NonEmptyListSerializer
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class TiltakDatadelingClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 0, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    private val getAvtalerForPersonCache: Cache<GraphqlRequest.GetAvtalerForPerson, List<Avtale>> =
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .recordStats()
            .build()

    suspend fun getAvtalerForPerson(
        requestInput: GraphqlRequest.GetAvtalerForPerson,
        accessType: AccessType,
    ): Either<TiltakDatadelingError, List<Avtale>> {
        getAvtalerForPersonCache.getIfPresent(requestInput)?.let { return@getAvtalerForPerson it.right() }

        val request = GraphqlRequest(
            query = """
                query(${'$'}norskIdent: String!) {
                    avtalerForPerson(personnummer: ${'$'}norskIdent) {
                        avtaleId
                        avtaleNr
                        deltakerFnr
                        bedriftNr
                        tiltakstype
                        startDato
                        sluttDato
                        avtaleStatus
                    }
                }
            """.trimIndent(),
            variables = requestInput,
        )
        return graphqlRequest<GraphqlRequest.GetAvtalerForPerson, GetAvtalerForPersonResponse>(
            request = request,
            accessType = accessType,
        )
            .map { it.avtalerForPerson }
            .onRight { getAvtalerForPersonCache.put(requestInput, it) }
    }

    private suspend inline fun <reified T, reified V> graphqlRequest(
        request: GraphqlRequest<T>,
        accessType: AccessType,
    ): Either<TiltakDatadelingError, V> {
        val response = client.post("$baseUrl/graphql") {
            bearerAuth(tokenProvider.exchange(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            return TiltakDatadelingError
                .InvalidGraphqlResponse(message = "Unexpected status: ${response.status}")
                .left()
        }

        val graphqlResponse: GraphqlResponse<V> = response.body()

        return if (graphqlResponse.errors != null) {
            TiltakDatadelingError.GraphqlError(errors = graphqlResponse.errors).left()
        } else if (graphqlResponse.data == null) {
            TiltakDatadelingError
                .InvalidGraphqlResponse(message = "Both errors and data are missing from response")
                .left()
        } else {
            return graphqlResponse.data.right()
        }
    }
}

sealed class TiltakDatadelingError {
    data class InvalidGraphqlResponse(val message: String) : TiltakDatadelingError()

    data class GraphqlError(val errors: Nel<GraphqlResponse.GraphqlError>) : TiltakDatadelingError()
}

@Serializable
data class GraphqlRequest<T>(
    val query: String,
    val variables: T,
) {
    @Serializable
    data class GetAvtalerForPerson(
        val norskIdent: String,
    )
}

@Serializable
data class GraphqlResponse<T>(
    val data: T? = null,
    @Serializable(with = NonEmptyListSerializer::class)
    val errors: Nel<GraphqlError>? = null,
) {
    @Serializable
    data class GraphqlError(
        val message: String,
        /**
         * Lokasjon til feilen kan være definert ved bl.a. ugyldig syntaks
         */
        val locations: List<Location>? = null,
        /**
         * Ekstra metadata relatert til feilen
         */
        val extensions: Extensions? = null,
    )

    @Serializable
    data class Location(
        val line: Int,
        val column: Int,
    )

    @Serializable
    data class Extensions(
        /**
         * Kategori av feilkode
         */
        val classification: String? = null,
    )
}

@Serializable
data class GetAvtalerForPersonResponse(
    val avtalerForPerson: List<Avtale>,
)

@Serializable
data class Avtale(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val avtaleNr: Int,
    val deltakerFnr: NorskIdent,
    val bedriftNr: Organisasjonsnummer,
    val tiltakstype: Tiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val avtaleStatus: Status,
) {
    enum class Tiltakstype {
        ARBEIDSTRENING,
        MIDLERTIDIG_LONNSTILSKUDD,
        VARIG_LONNSTILSKUDD,
        MENTOR,
        INKLUDERINGSTILSKUDD,
        SOMMERJOBB,
    }

    enum class Status {
        ANNULLERT,
        AVBRUTT,
        PAABEGYNT,
        MANGLER_GODKJENNING,
        KLAR_FOR_OPPSTART,
        GJENNOMFORES,
        AVSLUTTET,
    }
}
