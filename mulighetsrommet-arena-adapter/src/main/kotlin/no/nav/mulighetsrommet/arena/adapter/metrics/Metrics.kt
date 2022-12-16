package no.nav.mulighetsrommet.arena.adapter.metrics

import io.micrometer.core.instrument.AbstractTimer
import io.micrometer.core.instrument.Timer
import no.nav.mulighetsrommet.ktor.plugins.Metrikker
import java.util.concurrent.TimeUnit

suspend fun <A> Timer.recordSuspend(block: suspend () -> A): A =
    when (val timer = this) {
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
    fun replayArenaEventTimer(tags: String): Timer = Timer
        .builder("replay_arena_event_timer")
        .tags(tags)
        .register(Metrikker.appMicrometerRegistry)

    fun retryArenaEventTimer(tags: String): Timer = Timer
        .builder("retry_arena_event_timer")
        .tags(tags)
        .register(Metrikker.appMicrometerRegistry)

    fun processArenaEventTimer(tags: String): Timer = Timer
        .builder("process_arena_event_timer")
        .tags(tags)
        .register(Metrikker.appMicrometerRegistry)
}
