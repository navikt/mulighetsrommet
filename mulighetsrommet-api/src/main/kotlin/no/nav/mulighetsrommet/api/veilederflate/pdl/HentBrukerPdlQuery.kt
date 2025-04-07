package no.nav.mulighetsrommet.api.veilederflate.pdl

import arrow.core.Either
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class HentBrukerPdlQuery(
    private val pdl: PdlClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class PdlResponse(
        val hentPerson: PdlPerson? = null,
        val hentGeografiskTilknytning: PdlGeografiskTilknytning? = null,
    )

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

                require(it.hentGeografiskTilknytning != null) {
                    "hentGeografiskTilknytning var null og errors tom! response: $it"
                }

                val geografiskTilknytning = when (it.hentGeografiskTilknytning.gtType) {
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

sealed class GeografiskTilknytning {
    data class GtKommune(val value: String) : GeografiskTilknytning()
    data class GtBydel(val value: String) : GeografiskTilknytning()
    data class GtUtland(val value: String?) : GeografiskTilknytning()
    data object GtUdefinert : GeografiskTilknytning()
}
