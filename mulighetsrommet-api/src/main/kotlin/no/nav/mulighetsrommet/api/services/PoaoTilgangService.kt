package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.poao_tilgang.PoaoTilgangClient
import no.nav.mulighetsrommet.ktor.exception.StatusException
import java.util.concurrent.TimeUnit

class PoaoTilgangService(
    val client: PoaoTilgangClient,
) {

    private val cache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build()

    suspend fun verifyAccessToModia(navIdent: String) {
        val access = cachedResult(cache, navIdent) {
            client.hasAccessToModia(navIdent)
        }

        if (!access) {
            throw StatusException(HttpStatusCode.Forbidden, "Mangler tilgang til Modia Arbeidsrettet oppfølging")
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
