package no.nav.mulighetsrommet.arena.adapter.consumers

import kotlinx.serialization.json.JsonElement

abstract class TopicConsumer {
    abstract val topic: String

    open fun shouldProcessEvent(payload: JsonElement): Boolean = true

    abstract fun resolveKey(payload: JsonElement): String

    abstract fun processEvent(payload: JsonElement)
}
