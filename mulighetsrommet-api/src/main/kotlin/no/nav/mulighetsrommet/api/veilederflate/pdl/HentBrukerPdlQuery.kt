package no.nav.mulighetsrommet.api.veilederflate.pdl

import arrow.core.Either
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.concurrent.TimeUnit

class HentBrukerPdlQuery(
    private val pdl: PdlClient,
) {
    @Serializable
    data class PdlResponse(
        val hentPerson: Person? = null,
        val hentGeografiskTilknytning: PdlGeografiskTilknytning? = null,
    ) {
        @Serializable
        data class Person(
            val navn: List<PdlNavn>,
        )
    }

    private val hentPersonCache: Cache<PdlIdent, HentBrukerResponse> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentBruker(
        ident: PdlIdent,
        accessType: AccessType,
    ): Either<PdlError, HentBrukerResponse> {
        hentPersonCache.getIfPresent(ident)?.let { return@hentBruker it.right() }

        val request = GraphqlRequest(
            query = $$"""
                query($ident: ID!) {
                    hentPerson(ident: $ident) {
                        navn(historikk: false) {
                            fornavn
                        }
                    }
                    hentGeografiskTilknytning(ident: $ident) {
                        gtType
                        gtKommune
                        gtBydel
                        gtLand
                    }
                }
            """.trimIndent(),
            variables = GraphqlRequest.Ident(ident),
        )
        return pdl.graphqlRequest<GraphqlRequest.Ident, PdlResponse>(request, accessType)
            .map {
                require(it.hentPerson != null) {
                    "hentPerson var null og errors tom! response: $it"
                }

                val geografiskTilknytning = toGeografiskTilknytning(it.hentGeografiskTilknytning)

                HentBrukerResponse(it.hentPerson.navn.firstOrNull()?.fornavn, geografiskTilknytning)
            }
            .onRight {
                hentPersonCache.put(ident, it)
            }
    }
}

data class HentBrukerResponse(
    val fornavn: String?,
    val geografiskTilknytning: GeografiskTilknytning,
)
