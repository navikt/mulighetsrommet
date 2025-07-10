package no.nav.mulighetsrommet.ktor.clients.plugin

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.system.measureTimeMillis

class RateLimitTest : FunSpec({

    val engine = MockEngine.Companion { request ->
        respond("OK")
    }

    suspend fun TestScope.awaitAllRequests(requests: Int, client: HttpClient): Long = measureTimeMillis {
        (1..requests).map { async { client.get("https://test.no/") } }.awaitAll()
    }

    test("should complete immediately when requests are less than or equal to the window size") {
        val requests = 3
        val requestPerWindow = 3
        val windowSize = 200L

        val client = HttpClient(engine) {
            install(RateLimit) {
                maxRequestsPerWindow = requestPerWindow
                windowSizeMs = windowSize
            }
        }

        val elapsed = awaitAllRequests(requests, client)

        elapsed.shouldBeLessThan(windowSize)
    }

    test("should complete requests within two windows when requests requires more than one window") {
        val requests = 4
        val requestPerWindow = 3
        val windowSize = 500L

        val client = HttpClient(engine) {
            install(RateLimit) {
                maxRequestsPerWindow = requestPerWindow
                windowSizeMs = windowSize
            }
        }

        val elapsed = awaitAllRequests(requests, client)

        elapsed.shouldBeGreaterThanOrEqual(windowSize)
        elapsed.shouldBeLessThan(windowSize * 2)
    }

    test("should complete requests within three windows when requests requires more than two windows") {
        val requests = 7
        val requestPerWindow = 3
        val windowSize = 200L

        val client = HttpClient(engine) {
            install(RateLimit) {
                maxRequestsPerWindow = requestPerWindow
                windowSizeMs = windowSize
            }
        }

        val elapsed = awaitAllRequests(requests, client)

        elapsed.shouldBeGreaterThanOrEqual(windowSize * 2)
        elapsed.shouldBeLessThan(windowSize * 3)
    }
})
