package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.utils.CacheUtils
import no.nav.poao_tilgang.client.*
import java.util.*
import java.util.concurrent.TimeUnit

class PoaoTilgangService(
    val client: PoaoTilgangClient,
) {

    private val tilgangCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    fun verifyAccessToUserFromVeileder(
        navAnsattObjectId: UUID,
        norskIdent: NorskIdent,
    ) {
        val access = CacheUtils.tryCacheFirstNotNull(tilgangCache, "$navAnsattObjectId-${norskIdent.value}") {
            client.evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    navAnsattObjectId,
                    TilgangType.LESE,
                    norskIdent.value,
                ),
            )
                .getOrDefault(Decision.Deny("Veileder har ikke tilgang til bruker", "")).isPermit
        }

        if (!access) {
            throw StatusException(HttpStatusCode.Forbidden, "Mangler tilgang til bruker")
        }
    }

    fun verifyAccessToModia(navAnsattEntraObjectId: UUID) {
        val access = CacheUtils.tryCacheFirstNotNull(tilgangCache, navAnsattEntraObjectId.toString()) {
            client.evaluatePolicy(NavAnsattTilgangTilModiaPolicyInput(navAnsattEntraObjectId)).getOrThrow().isPermit
        }

        if (!access) {
            SecureLog.logger.warn("Veileder med EntraObjectId $navAnsattEntraObjectId har ikke tilgang til modia")
            throw StatusException(HttpStatusCode.Forbidden, "Mangler tilgang til Modia")
        }
    }
}
