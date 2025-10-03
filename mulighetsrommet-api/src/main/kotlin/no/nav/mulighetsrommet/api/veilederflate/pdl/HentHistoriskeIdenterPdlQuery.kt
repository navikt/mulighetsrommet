package no.nav.mulighetsrommet.api.veilederflate.pdl

import arrow.core.Either
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.concurrent.TimeUnit

class HentHistoriskeIdenterPdlQuery(
    private val pdl: PdlClient,
) {
    private val hentIdenterCache: Cache<GraphqlRequest.HentHistoriskeIdenter, HentIdenterResponse.Identliste> =
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .recordStats()
            .build()

    suspend fun hentHistoriskeIdenter(
        variables: GraphqlRequest.HentHistoriskeIdenter,
        accessType: AccessType,
    ): Either<PdlError, List<IdentInformasjon>> {
        hentIdenterCache.getIfPresent(variables)?.let { return@hentHistoriskeIdenter it.identer.right() }

        val request = GraphqlRequest(
            query = $$"""
                query($ident: ID!, $grupper: [IdentGruppe!]) {
                    hentIdenter(ident: $ident, grupper: $grupper, historikk: true) {
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
        return pdl.graphqlRequest<GraphqlRequest.HentHistoriskeIdenter, HentIdenterResponse>(request, accessType)
            .onRight { response ->
                response.hentIdenter?.also { hentIdenterCache.put(variables, it) }
            }
            .map { response ->
                requireNotNull(response.hentIdenter) {
                    "hentIdenter var null og errors tom! response: $response"
                }
                response.hentIdenter.identer
            }
    }
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
