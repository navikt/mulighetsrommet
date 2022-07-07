package no.nav.mulighetsrommet.arena.adapter.consumers

abstract class TopicConsumer<out T, in V>  {
    abstract val topic: String

    open fun shouldProcessEvent(payload: V): Boolean = true

    abstract fun resolveKey(payload: V): String

    abstract fun processEvent(payload: V)

    abstract fun toDomain(payload: String): T

}
