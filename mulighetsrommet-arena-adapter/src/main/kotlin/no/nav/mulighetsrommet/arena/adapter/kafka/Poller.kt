package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.coroutines.*

class Poller(private val delay: Long, val block: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            while (scope.isActive) {
                block()
                delay(delay)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
