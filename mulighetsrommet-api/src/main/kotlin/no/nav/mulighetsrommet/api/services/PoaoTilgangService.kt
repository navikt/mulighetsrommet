package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.secure_log.SecureLog
import no.nav.poao_tilgang.client.*
import java.util.*
import java.util.concurrent.TimeUnit

class PoaoTilgangService(
    val client: PoaoTilgangClient
) {

    private val cache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build()

    private val brukerAzureIdToAdGruppeCache: Cache<String, List<AdGruppe>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build()

    suspend fun verifyAccessToUserFromVeileder(navAnsattAzureId: UUID, norskIdent: String) {
        val access = cachedResult(cache, "$navAnsattAzureId-$norskIdent") {
            // TODO Hør med Sondre ang. error handling ved kasting av feil
            client.evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    navAnsattAzureId,
                    TilgangType.LESE,
                    norskIdent
                )
            )
                .getOrThrow().isPermit
        }

        if (!access) {
            throw StatusException(HttpStatusCode.Forbidden, "Veileder mangler tilgang til bruker")
        }
    }

    suspend fun verfiyAccessToModia(navAnsattAzureId: UUID) {
        val access = cachedResult(cache, navAnsattAzureId.toString()) {
            client.evaluatePolicy(NavAnsattTilgangTilModiaPolicyInput(navAnsattAzureId)).getOrThrow().isPermit
        }

        if (!access) {
            SecureLog.logger.warn("Veileder med navAnsattAzureId $navAnsattAzureId har ikke tilgang til modia")
            throw StatusException(HttpStatusCode.Forbidden, "Veileder har ikke tilgang til modia, se mer i secure logs")
        }
    }

    suspend fun hentAdGrupper(navAnsattAzureId: UUID): List<AdGruppe> {
        return cachedResult(brukerAzureIdToAdGruppeCache, navAnsattAzureId.toString()) {
            client.hentAdGrupper(navAnsattAzureId).getOrDefault { emptyList() }
        }
    }

    private suspend fun <K, V : Any> cachedResult(
        cache: Cache<K, V>,
        key: K,
        supplier: suspend () -> V
    ): V {
        val cachedValue = cache.getIfPresent(key)
        if (cachedValue != null) {
            return cachedValue
        }

        val value = supplier.invoke()
        cache.put(key, value)
        return value
    }
}
