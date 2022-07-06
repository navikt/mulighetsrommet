package no.nav.mulighetsrommet.arena.adapter.consumers

abstract class TopicConsumer<T> {
    abstract val topic: String

    open fun shouldProcessEvent(payload: T): Boolean = true

    abstract fun resolveKey(payload: T): String

    abstract fun processEvent(payload: T)
}
