package no.nav.mulighetsrommet.arena.adapter.metrics

import io.micrometer.core.instrument.AbstractTimer
import io.micrometer.core.instrument.Timer
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.concurrent.TimeUnit

suspend fun <A> Timer.recordSuspend(block: suspend () -> A): A = when (val timer = this) {
    is AbstractTimer -> timer.recordSuspendInternal(block)
    else -> block()
}

private suspend fun <A> AbstractTimer.recordSuspendInternal(block: suspend () -> A): A {
    val clock = Metrikker.appMicrometerRegistry.config().clock()
    val s = clock.monotonicTime()
    return try {
        block()
    } finally {
        val e = clock.monotonicTime()
        record(e - s, TimeUnit.NANOSECONDS)
    }
}

object Metrics {
    private const val ARENA_TABLE_TAG = "arena_table"

    fun retryArenaEventTimer(table: String): Timer = Timer
        .builder("retry_arena_event_timer")
        .tags(ARENA_TABLE_TAG, table)
        .register(Metrikker.appMicrometerRegistry)

    fun processArenaEventTimer(table: String): Timer = Timer
        .builder("process_arena_event_timer")
        .tags(ARENA_TABLE_TAG, table)
        .register(Metrikker.appMicrometerRegistry)
}
