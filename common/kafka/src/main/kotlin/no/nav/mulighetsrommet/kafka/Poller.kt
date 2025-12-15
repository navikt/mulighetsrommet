package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
