package no.nav.mulighetsrommet.ktor.clients.plugin

import io.ktor.client.plugins.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RateLimit")

/**
 * Rate limit plugin for Ktor clients.
 */
class RateLimitConfig {
    /**
     * Maximum number of requests allowed per window.
     */
    var maxRequestsPerWindow: Int = 500

    /**
     * Size of the time window in milliseconds during which the requests are counted.
     */
    var windowSizeMs: Long = 10_000L
}

val RateLimit = createClientPlugin("RateLimit", ::RateLimitConfig) {
    val rateLimiterChannel = Channel<CompletableDeferred<Unit>>(pluginConfig.maxRequestsPerWindow)
    val rateLimiterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val rateLimiterJob = rateLimiterScope.launch {
        val executionTimes = ArrayDeque<Long>()
        while (isActive) {
            val permit = rateLimiterChannel.receive()
            executionTimes.waitForPermit(pluginConfig.maxRequestsPerWindow, pluginConfig.windowSizeMs)
            permit.complete(Unit)
        }
    }

    onRequest { _, _ ->
        val permit = CompletableDeferred<Unit>()
        rateLimiterChannel.send(permit)
        permit.await()
    }

    onClose {
        rateLimiterJob.cancel()
        rateLimiterChannel.close()
    }
}

private suspend fun ArrayDeque<Long>.waitForPermit(
    maxRequestsPerWindow: Int,
    windowSizeMs: Long,
) {
    while (true) {
        val now = System.currentTimeMillis()

        // Remove timestamps that are outside the current window
        while (isNotEmpty() && first() <= now - windowSizeMs) {
            removeFirst()
        }

        // If we have not reached the limit, add the current timestamp and return
        if (size < maxRequestsPerWindow) {
            addLast(now)
            return
        }

        // If we have reached the limit, wait until the oldest request is outside the window
        val waitTime = first() + windowSizeMs - now
        log.info("Rate limit reached, waiting for $waitTime ms")
        delay(waitTime)
    }
}
