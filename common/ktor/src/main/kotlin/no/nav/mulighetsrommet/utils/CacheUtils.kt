package no.nav.mulighetsrommet.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

class CachedComputation<T : Any>(
    expireAfterWrite: Duration,
) {
    private val cache: Cache<Unit, T> = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(expireAfterWrite)
        .build()

    private val mutex = Mutex()

    suspend fun getOrCompute(compute: suspend () -> T): T {
        cache.getIfPresent(Unit)?.let { return it }

        return mutex.withLock {
            cache.getIfPresent(Unit) ?: compute().also { cache.put(Unit, it) }
        }
    }

    fun invalidate() {
        cache.invalidate(Unit)
    }
}

object CacheUtils {

    inline fun <K : Any, V : Any> tryCacheFirstNullable(cache: Cache<K, V>, key: K, valueSupplier: () -> V?): V? {
        val value = cache.getIfPresent(key)

        if (value == null) {
            val newValue: V = valueSupplier.invoke() ?: return null
            cache.put(key, newValue)
            return newValue
        }

        return value
    }

    inline fun <K : Any, V : Any> tryCacheFirstNotNull(cache: Cache<K, V>, key: K, valueSupplier: () -> V): V {
        return tryCacheFirstNullable(cache, key) { valueSupplier.invoke() }!!
    }
}
