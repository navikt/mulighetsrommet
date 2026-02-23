package no.nav.mulighetsrommet.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class CachedComputationTest : FunSpec({

    test("beregning blir cachet og gjenbrukt ved påfølgende kall") {
        var computeCount = 0
        val cache = CachedComputation<Int>(expireAfterWrite = Duration.ofMinutes(1))

        val result1 = cache.getOrCompute {
            computeCount++
            42
        }
        val result2 = cache.getOrCompute {
            computeCount++
            99
        }

        result1 shouldBe 42
        result2 shouldBe 42
        computeCount shouldBe 1
    }

    test("invalidate fjerner cachet verdi og tvinger ny beregning") {
        var computeCount = 0
        val cache = CachedComputation<String>(expireAfterWrite = Duration.ofMinutes(1))

        val result1 = cache.getOrCompute {
            computeCount++
            "første"
        }

        cache.invalidate()

        val result2 = cache.getOrCompute {
            computeCount++
            "andre"
        }

        result1 shouldBe "første"
        result2 shouldBe "andre"
        computeCount shouldBe 2
    }

    test("cachet verdi utløper etter konfigurert tid") {
        var computeCount = 0
        val cache = CachedComputation<Int>(expireAfterWrite = Duration.ofMillis(50))

        val result1 = cache.getOrCompute {
            computeCount++
            1
        }

        delay(100)

        val result2 = cache.getOrCompute {
            computeCount++
            2
        }

        result1 shouldBe 1
        result2 shouldBe 2
        computeCount shouldBe 2
    }

    test("mutex forhindrer samtidige beregninger når flere coroutines kaller get samtidig") {
        val computeCount = AtomicInteger(0)
        val cache = CachedComputation<Int>(expireAfterWrite = Duration.ofMinutes(1))

        val results = (1..10).map {
            async {
                cache.getOrCompute {
                    computeCount.incrementAndGet()
                    delay(50)
                    42
                }
            }
        }.awaitAll()

        results.forEach { it shouldBe 42 }

        computeCount.get() shouldBe 1
    }
})
