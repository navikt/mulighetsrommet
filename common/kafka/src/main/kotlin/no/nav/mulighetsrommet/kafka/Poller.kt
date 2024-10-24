package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.*

class Poller(private val delay: Long, val block: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        block()
        scope.launch {
            while (scope.isActive) {
                delay(delay)
                block()
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
