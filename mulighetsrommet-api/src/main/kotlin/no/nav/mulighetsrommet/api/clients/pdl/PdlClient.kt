package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.common.client.pdl.Tema
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Team Valps behandlingsnummer for behandling av data for løsningen Arbeidsmarkedstiltak i Modia
 */
const val VALP_BEHANDLINGSNUMMER: String = "B450"

class PdlClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    private val hentIdenterCache: Cache<GraphqlRequest.HentHistoriskeIdenter, List<IdentInformasjon>> =
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .recordStats()
            .build()

    private val hentPersonCache: Cache<PdlIdent, PdlPerson> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    private val hentGeografiskTilknytningCache: Cache<PdlIdent, GeografiskTilknytning> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentHistoriskeIdenter(
        variables: GraphqlRequest.HentHistoriskeIdenter,
        accessType: AccessType,
    ): Either<PdlError, List<IdentInformasjon>> {
        hentIdenterCache.getIfPresent(variables)?.let { return@hentHistoriskeIdenter it.right() }

        val request = GraphqlRequest(
            query = """
                query(${'$'}ident: ID!, ${'$'}grupper: [IdentGruppe!]) {
                    hentIdenter(ident: ${'$'}ident, grupper: ${'$'}grupper, historikk: true) {
                        identer {
                            ident,
                            historisk,
                            gruppe
                        }
                    }
                }
            """.trimIndent(),
            variables = variables,
        )
        return graphqlRequest<GraphqlRequest.HentHistoriskeIdenter, HentIdenterResponse>(request, accessType)
            .map {
                require(it.hentIdenter != null) {
                    "hentIdenter var null og errors tom! response: $it"
                }
                it.hentIdenter.identer
            }
            .onRight { hentIdenterCache.put(variables, it) }
    }

    suspend fun hentPerson(ident: PdlIdent, accessType: AccessType): Either<PdlError, PdlPerson> {
        hentPersonCache.getIfPresent(ident)?.let { return@hentPerson it.right() }

        val request = GraphqlRequest(
            query = """
                query(${'$'}ident: ID!) {
                    hentPerson(ident: ${'$'}ident) {
                    	navn(historikk: false) {
                            fornavn
                            mellomnavn
                            etternavn
                        }
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Ident(ident),
        )
        return graphqlRequest<GraphqlRequest.Ident, HentPersonResponse>(request, accessType)
            .map {
                require(it.hentPerson != null) {
                    "hentPerson var null og errors tom! response: $it"
                }
                it.hentPerson
            }
            .onRight {
                hentPersonCache.put(ident, it)
            }
    }

    suspend fun hentPersonBolk(identer: Set<PdlIdent>): Either<PdlError, Map<PdlIdent, PdlPerson>> {
        val request = GraphqlRequest(
            query = """
                query(${'$'}identer: [ID!]!) {
                    hentPersonBolk(identer: ${'$'}identer) {
                        ident,
                        person {
                            navn {
                                fornavn
                                mellomnavn
                                etternavn
                            }
                        },
                        code
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Identer(identer),
        )
        return graphqlRequest<GraphqlRequest.Identer, HentPersonBolkResponse>(request, AccessType.M2M)
            .map { response ->
                response.hentPersonBolk
                    .mapNotNull {
                        when (it.code) {
                            HentPersonBolkResponse.PersonBolkResponseEntry.Code.OK -> {
                                val person = requireNotNull(it.person) {
                                    "person forventet siden response var OK"
                                }
                                PdlIdent(it.ident) to PdlPerson(person.navn)
                            }

                            else -> {
                                log.error("Response med ${it.code} fra pdl. Se secure logs for detaljer.")
                                SecureLog.logger.error("Response med ${it.code} fra pdl for ident=${it.ident}")
                                null
                            }
                        }
                    }
                    .toMap()
            }
    }

    suspend fun hentGeografiskTilknytning(
        ident: PdlIdent,
        accessType: AccessType,
    ): Either<PdlError, GeografiskTilknytning> {
        hentGeografiskTilknytningCache.getIfPresent(ident)?.let { return@hentGeografiskTilknytning it.right() }

        val request = GraphqlRequest(
            query = """
                query(${'$'}ident: ID!) {
                    hentGeografiskTilknytning(ident: ${'$'}ident) {
                        gtType
                        gtKommune
                        gtBydel
                        gtLand
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Ident(ident),
        )
        return graphqlRequest<GraphqlRequest.Ident, HentGeografiskTilknytningResponse>(request, accessType)
            .map {
                require(it.hentGeografiskTilknytning != null) {
                    "hentGeografiskTilknytning var null og errors tom! response: $it"
                }
                when (it.hentGeografiskTilknytning.gtType) {
                    TypeGeografiskTilknytning.BYDEL -> {
                        GeografiskTilknytning.GtBydel(requireNotNull(it.hentGeografiskTilknytning.gtBydel))
                    }

                    TypeGeografiskTilknytning.KOMMUNE -> {
                        GeografiskTilknytning.GtKommune(requireNotNull(it.hentGeografiskTilknytning.gtKommune))
                    }

                    TypeGeografiskTilknytning.UTLAND -> {
                        log.warn("Pdl returnerte UTLAND geografisk tilkytning. Da kan man ikke hente enhet fra norg.")
                        GeografiskTilknytning.GtUtland(it.hentGeografiskTilknytning.gtLand)
                    }

                    TypeGeografiskTilknytning.UDEFINERT -> {
                        log.warn("Pdl returnerte UDEFINERT geografisk tilkytning. Da kan man ikke hente enhet fra norg.")
                        GeografiskTilknytning.GtUdefinert
                    }
                }
            }
            .onRight {
                hentGeografiskTilknytningCache.put(ident, it)
            }
    }

    private suspend inline fun <reified T, reified V> graphqlRequest(
        req: GraphqlRequest<T>,
        accessType: AccessType,
    ): Either<PdlError, V> {
        val response = client.post("$baseUrl/graphql") {
            bearerAuth(tokenProvider.exchange(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Behandlingsnummer", VALP_BEHANDLINGSNUMMER)
            header("Tema", Tema.GEN)
            setBody(req)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Error fra pdl: $response")
        }

        val graphqlResponse: GraphqlResponse<V> = response.body()

        return if (graphqlResponse.errors.isNotEmpty()) {
            if (graphqlResponse.errors.any { error -> error.extensions?.code == PdlErrorCode.NOT_FOUND }) {
                PdlError.NotFound.left()
            } else {
                log.error("Error fra pdl: ${graphqlResponse.errors}")
                PdlError.Error.left()
            }
        } else {
            val data = requireNotNull(graphqlResponse.data) {
                "forventet data siden errors var tom"
            }
            data.right()
        }
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
    data class Identer(
        val identer: Set<PdlIdent>,
    )

    @Serializable
    data class Ident(
        val ident: PdlIdent,
    )

    @Serializable
    data class HentHistoriskeIdenter(
        val ident: PdlIdent,
        val grupper: List<IdentGruppe>,
    )
}

@Serializable
data class GraphqlResponse<T>(
    val data: T? = null,
    val errors: List<GraphqlError> = emptyList(),
) {
    @Serializable
    data class GraphqlError(
        val message: String? = null,
        /**
         * Ekstra metadata relatert til feilen
         */
        val extensions: Extensions? = null,
    )

    @Serializable
    data class Extensions(
        /**
         * Feilkode fra PDL, kun til til stedet om dette er en feil spesifikt for PDL
         */
        val code: PdlErrorCode? = null,
        /**
         * Kategori av feilkode
         */
        val classification: String? = null,
    )
}

enum class PdlErrorCode {
    /**
     * Ikke gyldig token.
     */
    @SerialName("unauthenticated")
    UNAUTHENTICATED,

    /**
     * Gyldig, men feil type token eller ikke tilgang til tjenesten.
     */
    @SerialName("unauthorized")
    UNAUTHORIZED,

    /**
     * Fant ikke person i PDL.
     */
    @SerialName("not_found")
    NOT_FOUND,

    /**
     * Ugyldig ident. Ugyldig spørring. For stor bolkspørring.
     */
    @SerialName("bad_request")
    BAD_REQUEST,

    /**
     * Intern feil i Api.
     */
    @SerialName("server_error")
    SERVER_ERROR,
}

@Serializable
data class HentIdenterResponse(
    val hentIdenter: Identliste? = null,
) {

    @Serializable
    data class Identliste(
        val identer: List<IdentInformasjon>,
    )
}

@Serializable
@JvmInline
value class PdlIdent(val value: String)

@Serializable
data class IdentInformasjon(
    val ident: PdlIdent,
    val gruppe: IdentGruppe,
    val historisk: Boolean,
)

@Serializable
enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

@Serializable
data class HentPersonResponse(
    val hentPerson: PdlPerson? = null,
)

@Serializable
data class HentPersonBolkResponse(
    val hentPersonBolk: List<PersonBolkResponseEntry>,
) {
    @Serializable
    data class PersonBolkResponseEntry(
        val ident: String,
        val person: PdlPerson?,
        val code: Code,
    ) {

        enum class Code {
            @SerialName("ok")
            OK,

            /**
             * Fant ikke person.
             */
            @SerialName("not_found")
            NOT_FOUND,

            /**
             * Ugyldig ident.
             */
            @SerialName("bad_request")
            BAD_REQUEST,
        }
    }
}

@Serializable
data class PdlPerson(
    val navn: List<PdlNavn>,
) {
    @Serializable
    data class PdlNavn(
        val fornavn: String? = null,
        val mellomnavn: String? = null,
        val etternavn: String? = null,
    )
}

@Serializable
data class HentGeografiskTilknytningResponse(
    val hentGeografiskTilknytning: PdlGeografiskTilknytning? = null,
)

sealed class GeografiskTilknytning {
    data class GtKommune(val value: String) : GeografiskTilknytning()
    data class GtBydel(val value: String) : GeografiskTilknytning()
    data class GtUtland(val value: String?) : GeografiskTilknytning()
    data object GtUdefinert : GeografiskTilknytning()
}

@Serializable
data class PdlGeografiskTilknytning(
    val gtType: TypeGeografiskTilknytning,
    val gtLand: String? = null,
    val gtKommune: String? = null,
    val gtBydel: String? = null,
)

enum class TypeGeografiskTilknytning {
    BYDEL,
    KOMMUNE,
    UDEFINERT,
    UTLAND,
}
